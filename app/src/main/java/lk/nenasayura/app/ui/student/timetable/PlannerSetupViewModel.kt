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
import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.LearningStyle
import lk.nenasayura.app.data.model.PreferredWindow
import lk.nenasayura.app.data.model.StudyGoal
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.StudentProfileRepository
import lk.nenasayura.app.data.repository.StudyPlanRepository

const val PLANNER_SETUP_STEPS = 4

/** Everything the setup wizard shows and the answers typed so far. */
data class PlannerSetupUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val subjects: List<Subject> = emptyList(),
    val profileExamYear: Int = 0,
    /** True when the student already has saved settings and is editing them. */
    val isEditing: Boolean = false,
    val step: Int = 0,
    val confidence: Map<String, Int> = emptyMap(),
    val availableDays: Set<Int> = emptySet(),
    val dailyMinutes: Map<Int, Int> = emptyMap(),
    val preferredWindow: PreferredWindow = PreferredWindow.EVENING,
    val sessionMinutes: Int = 45,
    val learningStyle: LearningStyle = LearningStyle.BALANCED,
    val goal: StudyGoal = StudyGoal.MARKS,
    val examDateMillis: Long = 0L,
    val stepError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isSaved: Boolean = false
)

class PlannerSetupViewModel : ViewModel() {

    private val profileRepository = StudentProfileRepository()
    private val planRepository = StudyPlanRepository()

    private val _uiState = MutableStateFlow(PlannerSetupUiState())
    val uiState: StateFlow<PlannerSetupUiState> = _uiState.asStateFlow()

    private var uid = ""
    private var generatedUntil = ""
    private var weightAdjustment: Map<String, Double> = emptyMap()
    private var lastReviewedWeekStart = ""

    init {
        load()
    }

    /** Loads the student's subjects from the profile and any settings saved earlier. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                val profile = profileRepository.getProfile()
                uid = profile.uid
                // subjectIds and subjectNames are saved in the same order
                val subjects = profile.subjectIds.mapIndexed { index, id ->
                    Subject(id = id, name = profile.subjectNames.getOrNull(index) ?: id)
                }
                val existing = planRepository.getSettings(profile.uid)
                generatedUntil = existing?.generatedUntil ?: ""
                weightAdjustment = existing?.weightAdjustment ?: emptyMap()
                lastReviewedWeekStart = existing?.lastReviewedWeekStart ?: ""

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        subjects = subjects,
                        profileExamYear = profile.examYear,
                        isEditing = existing != null,
                        confidence = subjects.associate { subject ->
                            subject.id to (existing?.confidence?.get(subject.id) ?: Confidence.OK.level)
                        },
                        availableDays = existing?.availableDays?.toSet()?.takeIf { days -> days.isNotEmpty() }
                            ?: setOf(1, 2, 3, 4, 5),
                        dailyMinutes = (1..7).associateWith { day ->
                            existing?.dailyMinutes?.get(day) ?: defaultMinutes(day)
                        },
                        preferredWindow = existing?.preferredWindow ?: PreferredWindow.EVENING,
                        sessionMinutes = existing?.sessionMinutes ?: 45,
                        learningStyle = existing?.learningStyle ?: LearningStyle.BALANCED,
                        goal = existing?.goal ?: StudyGoal.MARKS,
                        examDateMillis = existing?.examDateMillis ?: 0L
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = "Could not load your subjects. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    /** Called when the screen comes back (for example after choosing subjects in the profile). */
    fun onResume() {
        val state = _uiState.value
        if (!state.isLoading && state.subjects.isEmpty()) load()
    }

    // ---------- answers ----------

    fun setConfidence(subjectId: String, level: Int) = _uiState.update {
        it.copy(confidence = it.confidence + (subjectId to level), stepError = null)
    }

    fun toggleDay(day: Int) = _uiState.update {
        val days = if (day in it.availableDays) it.availableDays - day else it.availableDays + day
        it.copy(availableDays = days, stepError = null)
    }

    /** Adds or removes [delta] minutes (30 at a time) for one day, between 30 minutes and 8 hours. */
    fun changeMinutes(day: Int, delta: Int) = _uiState.update {
        val current = it.dailyMinutes[day] ?: defaultMinutes(day)
        val updated = (current + delta).coerceIn(30, 480)
        it.copy(dailyMinutes = it.dailyMinutes + (day to updated), stepError = null)
    }

    fun setWindow(window: PreferredWindow) = _uiState.update { it.copy(preferredWindow = window, stepError = null) }
    fun setSessionMinutes(minutes: Int) = _uiState.update { it.copy(sessionMinutes = minutes, stepError = null) }
    fun setLearningStyle(style: LearningStyle) = _uiState.update { it.copy(learningStyle = style) }
    fun setGoal(goal: StudyGoal) = _uiState.update { it.copy(goal = goal) }
    fun setExamDate(millis: Long) = _uiState.update { it.copy(examDateMillis = millis, stepError = null) }

    // ---------- moving between steps ----------

    /** Goes to the next step, or shows what is missing on this one. */
    fun next() {
        val state = _uiState.value
        val error = validate(state, state.step)
        if (error != null) {
            _uiState.update { it.copy(stepError = error) }
        } else {
            _uiState.update {
                it.copy(step = (it.step + 1).coerceAtMost(PLANNER_SETUP_STEPS - 1), stepError = null)
            }
        }
    }

    /** Goes back one step. Returns false when already on the first step. */
    fun back(): Boolean {
        if (_uiState.value.step == 0) return false
        _uiState.update { it.copy(step = it.step - 1, stepError = null) }
        return true
    }

    /** Checks every step, then saves the settings. */
    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        for (step in 0 until PLANNER_SETUP_STEPS) {
            val error = validate(state, step)
            if (error != null) {
                _uiState.update { it.copy(step = step, stepError = error) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                // Without internet a write would wait forever, so give up after 15 seconds
                withTimeout(SAVE_TIMEOUT_MS) {
                    planRepository.saveSettings(
                        uid,
                        state.toSettings(generatedUntil, weightAdjustment, lastReviewedWeekStart)
                    )
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveError = "Could not reach the server. Check your internet connection and try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, saveError = "Could not save your plan settings. Please try again.")
                }
            }
        }
    }

    // ---------- rules ----------

    private fun validate(state: PlannerSetupUiState, step: Int): String? = when (step) {
        0 -> if (state.subjects.isEmpty()) "Choose your subjects in your profile first." else null
        1 -> if (state.availableDays.isEmpty()) "Choose at least one study day." else null
        2 -> {
            val tooShort = state.availableDays.any { (state.dailyMinutes[it] ?: 0) < state.sessionMinutes }
            if (tooShort) {
                "Some study days are shorter than one session. Add more time or choose a shorter session."
            } else null
        }
        else -> when {
            state.examDateMillis <= 0L -> "Please choose your exam date."
            daysUntilExam(state.examDateMillis) <= 0 -> "The exam date must be in the future."
            else -> null
        }
    }

    private fun PlannerSetupUiState.toSettings(
        generatedUntil: String,
        weightAdjustment: Map<String, Double>,
        lastReviewedWeekStart: String
    ): StudyPlanSettings {
        val days = availableDays.sorted()
        return StudyPlanSettings(
            confidence = subjects.associate { it.id to (confidence[it.id] ?: Confidence.OK.level) },
            availableDays = days,
            dailyMinutes = days.associateWith { dailyMinutes[it] ?: defaultMinutes(it) },
            preferredWindow = preferredWindow,
            sessionMinutes = sessionMinutes,
            learningStyle = learningStyle,
            goal = goal,
            examDateMillis = examDateMillis,
            weightAdjustment = weightAdjustment,
            lastReviewedWeekStart = lastReviewedWeekStart,
            generatedUntil = generatedUntil
        )
    }

    // Weekdays start at 2 hours, Saturday and Sunday at 4 hours
    private fun defaultMinutes(day: Int): Int = if (day >= 6) 240 else 120

    private companion object {
        const val SAVE_TIMEOUT_MS = 15_000L
    }
}