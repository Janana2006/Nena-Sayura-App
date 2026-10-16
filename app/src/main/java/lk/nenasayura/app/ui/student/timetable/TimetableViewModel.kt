package lk.nenasayura.app.ui.student.timetable



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.repository.StudentProfileRepository
import lk.nenasayura.app.data.repository.StudyPlanRepository

data class TimetableUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** null = the student has not set up a plan yet. */
    val settings: StudyPlanSettings? = null,
    /** subjectId -> subject name, from the profile */
    val subjectNames: Map<String, String> = emptyMap()
)

class TimetableViewModel : ViewModel() {

    private val profileRepository = StudentProfileRepository()
    private val planRepository = StudyPlanRepository()

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    /** Loads the saved settings. Called when the screen appears and when it comes back. */
    fun load() {
        viewModelScope.launch {
            // The spinner shows only while there is nothing to show yet
            _uiState.update { it.copy(isLoading = it.settings == null, errorMessage = null) }
            try {
                val profile = profileRepository.getProfile()
                val settings = planRepository.getSettings(profile.uid)
                val names = profile.subjectIds.mapIndexed { index, id ->
                    id to (profile.subjectNames.getOrNull(index) ?: id)
                }.toMap()
                _uiState.update {
                    it.copy(isLoading = false, settings = settings, subjectNames = names)
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
}