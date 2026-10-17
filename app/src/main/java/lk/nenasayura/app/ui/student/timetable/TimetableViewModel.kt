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
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.StudentProfileRepository
import lk.nenasayura.app.data.repository.StudyPlanRepository
import java.time.LocalDate
import lk.nenasayura.app.core.planner.ResourceAttacher
import lk.nenasayura.app.data.repository.StudyResourceRepository

data class TimetableUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val uid: String = "",
    /** The student's subjects, from the profile. */
    val subjects: List<Subject> = emptyList(),
    /** null = the student has not set up a plan yet. */
    val settings: StudyPlanSettings? = null,
    /** Sessions from today until the end of the generated plan. */
    val sessions: List<StudySession> = emptyList(),
    val isGenerating: Boolean = false,
    val generateError: String? = null
) {
    val subjectNames: Map<String, String> get() = subjects.associate { it.id to it.name }
}

class TimetableViewModel : ViewModel() {

    private val profileRepository = StudentProfileRepository()
    private val planRepository = StudyPlanRepository()
    private val resourceRepository = StudyResourceRepository()

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    /** Loads the saved settings and the planned sessions. Called when the screen appears and comes back. */
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
                val sessions = if (settings != null && settings.hasPlan) {
                    planRepository.getSessions(profile.uid, LocalDate.now().toString(), settings.generatedUntil)
                } else emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        uid = profile.uid,
                        subjects = subjects,
                        settings = settings,
                        sessions = sessions
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
        const val GENERATE_TIMEOUT_MS = 45_000L
    }
}