package lk.nenasayura.app.ui.student.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import lk.nenasayura.app.core.planner.PlanGenerator
import lk.nenasayura.app.core.planner.ResourceAttacher
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.StudentProfileRepository
import lk.nenasayura.app.data.repository.StudyPlanRepository
import lk.nenasayura.app.data.repository.StudyResourceRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class TimetableView { DAY, WEEK }

/** The Monday of the week that contains [date]. */
fun weekStartOf(date: LocalDate): LocalDate =
    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class TimetableUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val uid: String = "",
    /** The student's subjects, from the profile. */
    val subjects: List<Subject> = emptyList(),
    /** null = the student has not set up a plan yet. */
    val settings: StudyPlanSettings? = null,
    val viewMode: TimetableView = TimetableView.DAY,
    val selectedDate: LocalDate = LocalDate.now(),
    /** The sessions of the week that contains [selectedDate]. */
    val weekSessions: List<StudySession> = emptyList(),
    val isWeekLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generateError: String? = null,
    /** A short message shown once (as a toast), for example "Session moved". */
    val actionMessage: String? = null
) {
    val subjectNames: Map<String, String> get() = subjects.associate { it.id to it.name }

    val weekStart: LocalDate get() = weekStartOf(selectedDate)
    val weekDays: List<LocalDate> get() = (0L..6L).map { weekStart.plusDays(it) }

    val daySessions: List<StudySession>
        get() = weekSessions.filter { it.date == selectedDate.toString() }.sortedBy { it.startMinute }

    val doneCount: Int get() = weekSessions.count { it.status == SessionStatus.DONE }
    val missedCount: Int get() = weekSessions.count { it.status == SessionStatus.MISSED }
}

class TimetableViewModel : ViewModel() {

    private val profileRepository = StudentProfileRepository()
    private val planRepository = StudyPlanRepository()
    private val resourceRepository = StudyResourceRepository()

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    // ---------- loading ----------

    /** Loads the settings and the visible week. Called when the screen appears and when it comes back. */
    fun load() {
        viewModelScope.launch {
            // The spinner shows only while there is nothing to show yet
            _uiState.update { it.copy(isLoading = it.settings == null, errorMessage = null) }
            try {
                val profile = profileRepository.getProfile()
                val subjects = profile.subjectIds.mapIndexed { index, id ->
                    Subject(id = id, name = profile.subjectNames.getOrNull(index) ?: id)
                }
                val settings = planRepository.getSettings(profile.uid)
                val weekStart = _uiState.value.weekStart
                val sessions = if (settings != null && settings.hasPlan) {
                    fetchWeek(profile.uid, weekStart)
                } else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        uid = profile.uid,
                        subjects = subjects,
                        settings = settings,
                        // Ignore the result if the student already moved to another week
                        weekSessions = if (it.weekStart == weekStart) sessions else it.weekSessions
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load your timetable. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    private suspend fun fetchWeek(uid: String, weekStart: LocalDate): List<StudySession> =
        planRepository.getSessions(uid, weekStart.toString(), weekStart.plusDays(6).toString())

    // ---------- choosing what to look at ----------

    fun setViewMode(mode: TimetableView) = _uiState.update { it.copy(viewMode = mode) }

    fun selectDate(date: LocalDate) {
        val oldWeek = _uiState.value.weekStart
        _uiState.update { it.copy(selectedDate = date) }
        if (weekStartOf(date) != oldWeek) reloadWeek()
    }

    /** One week forward (1) or back (-1), keeping the same weekday. */
    fun shiftWeek(weeks: Int) = selectDate(_uiState.value.selectedDate.plusWeeks(weeks.toLong()))

    fun goToToday() = selectDate(LocalDate.now())

    private fun reloadWeek() {
        val state = _uiState.value
        val settings = state.settings ?: return
        if (!settings.hasPlan || state.uid.isBlank()) return
        val weekStart = state.weekStart

        viewModelScope.launch {
            // Clear first, so the old week's sessions never show under the new dates
            _uiState.update { it.copy(isWeekLoading = true, weekSessions = emptyList()) }
            try {
                val sessions = fetchWeek(state.uid, weekStart)
                _uiState.update {
                    if (it.weekStart == weekStart) it.copy(isWeekLoading = false, weekSessions = sessions) else it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isWeekLoading = false, actionMessage = "Could not load this week. Check your internet connection.")
                }
            }
        }
    }

    // ---------- changing sessions ----------

    /** Marks a session done, missed or planned. The screen updates at once; the save happens behind it. */
    fun setStatus(session: StudySession, status: SessionStatus) {
        val uid = _uiState.value.uid
        if (uid.isBlank() || session.id.isBlank()) return
        val completedAt = if (status == SessionStatus.DONE) System.currentTimeMillis() else 0L
        replaceSession(session.id) { it.copy(status = status, completedAtMillis = completedAt) }

        viewModelScope.launch {
            try {
                withTimeout(WRITE_TIMEOUT_MS) { planRepository.updateStatus(uid, session.id, status) }
            } catch (e: TimeoutCancellationException) {
                // Offline: the change is kept on the phone and syncs later
                _uiState.update { it.copy(actionMessage = OFFLINE_MESSAGE) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                replaceSession(session.id) { session } // put the old state back
                _uiState.update { it.copy(actionMessage = "Could not save the change. Please try again.") }
            }
        }
    }

    private fun replaceSession(id: String, transform: (StudySession) -> StudySession) {
        _uiState.update { state ->
            state.copy(weekSessions = state.weekSessions.map { if (it.id == id) transform(it) else it })
        }
    }

    /** Moves a session to [newDate], after the sessions already on that day. */
    fun moveSession(session: StudySession, newDate: LocalDate) {
        val state = _uiState.value
        val settings = state.settings ?: return
        if (state.uid.isBlank() || newDate.toString() == session.date) return

        viewModelScope.launch {
            writeThenRefresh {
                val others = planRepository.getSessions(state.uid, newDate.toString(), newDate.toString())
                val start = nextFreeStart(others, session.durationMinutes, settings.preferredWindow.startMinute)
                planRepository.moveSession(state.uid, session, newDate.toString(), start)
            }
            _uiState.update { it.copy(actionMessage = "Moved to ${formatDayLong(newDate)}") }
        }
    }

    /** Adds the student's own session (for example "Chemistry lab report"). */
    fun addCustomSession(title: String, subject: Subject?, date: LocalDate, durationMinutes: Int) {
        val state = _uiState.value
        val settings = state.settings ?: return
        if (state.uid.isBlank() || title.isBlank()) return

        viewModelScope.launch {
            writeThenRefresh {
                val sameDay = planRepository.getSessions(state.uid, date.toString(), date.toString())
                val start = nextFreeStart(sameDay, durationMinutes, settings.preferredWindow.startMinute)
                planRepository.addSessions(
                    state.uid,
                    listOf(
                        StudySession(
                            date = date.toString(),
                            startMinute = start,
                            durationMinutes = durationMinutes,
                            subjectId = subject?.id ?: "",
                            subjectName = subject?.name ?: "",
                            kind = SessionKind.CUSTOM,
                            resourceTitle = title.trim()
                        )
                    )
                )
            }
            selectDate(date) // show the day it was added to
        }
    }

    /** Deletes one of the student's own sessions. Planned (generated) sessions are not deleted. */
    fun deleteSession(session: StudySession) {
        val uid = _uiState.value.uid
        if (uid.isBlank() || session.kind != SessionKind.CUSTOM) return
        viewModelScope.launch {
            writeThenRefresh { planRepository.deleteSession(uid, session.id) }
        }
    }

    // Runs a write with a timeout, then reloads the visible week (from the phone's copy if offline)
    private suspend fun writeThenRefresh(block: suspend () -> Unit) {
        try {
            withTimeout(WRITE_TIMEOUT_MS) { block() }
        } catch (e: TimeoutCancellationException) {
            _uiState.update { it.copy(actionMessage = OFFLINE_MESSAGE) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(actionMessage = "Could not save the change. Please try again.") }
        }
        refreshWeek()
    }

    private suspend fun refreshWeek() {
        val state = _uiState.value
        if (state.uid.isBlank()) return
        val weekStart = state.weekStart
        try {
            val sessions = fetchWeek(state.uid, weekStart)
            _uiState.update { if (it.weekStart == weekStart) it.copy(weekSessions = sessions) else it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The screen keeps what it already shows
        }
    }

    /** Where a new or moved session starts: after the last session of that day, or at the preferred time. */
    private fun nextFreeStart(others: List<StudySession>, duration: Int, preferredStart: Int): Int {
        val lastEnd = others.maxOfOrNull { it.startMinute + it.durationMinutes }
        val start = if (lastEnd == null) preferredStart else lastEnd + BREAK_MINUTES
        return start.coerceIn(0, (24 * 60 - duration).coerceAtLeast(0))
    }

    fun clearMessage() = _uiState.update { it.copy(actionMessage = null) }

    // ---------- generating ----------

    /**
     * Builds the next 4 weeks (starting tomorrow) and saves them.
     * Planned sessions from tomorrow on are replaced; done and missed ones stay as history.
     */
    fun generate() {
        val state = _uiState.value
        val settings = state.settings ?: return
        if (state.isGenerating || state.uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generateError = null) }
            try {
                val startDate = LocalDate.now().plusDays(1)
                val generatedUntil = PlanGenerator.horizonEnd(startDate).minusDays(1).toString()

                // Reading the resources and several writes, so a longer timeout than a single save
                withTimeout(GENERATE_TIMEOUT_MS) {
                    val pool = resourceRepository.loadPool(state.uid, state.subjects.map { it.id }.toSet())
                    val completed = resourceRepository.getCompletedKeys(state.uid)
                    val sessions = ResourceAttacher.attach(
                        PlanGenerator.generate(settings, state.subjects, startDate, seed = state.uid),
                        pool,
                        completed
                    )

                    planRepository.deletePlannedFrom(state.uid, startDate.toString())
                    planRepository.addSessions(state.uid, sessions)
                    planRepository.saveSettings(state.uid, settings.copy(generatedUntil = generatedUntil))
                }
                _uiState.update { it.copy(isGenerating = false) }
                load()
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generateError = "Could not reach the server. Check your internet connection and try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGenerating = false, generateError = "Could not create your timetable. Please try again.")
                }
            }
        }
    }

    private companion object {
        const val WRITE_TIMEOUT_MS = 10_000L
        const val GENERATE_TIMEOUT_MS = 45_000L
        const val BREAK_MINUTES = 10
        const val OFFLINE_MESSAGE = "You seem to be offline. The change is saved on this phone and will sync later."
    }
}