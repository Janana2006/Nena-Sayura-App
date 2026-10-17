package lk.nenasayura.app.ui.student.progress



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.progress.ProgressAnalyzer
import lk.nenasayura.app.core.progress.ProgressReport
import lk.nenasayura.app.data.repository.QuizAttemptRepository
import lk.nenasayura.app.data.repository.QuizRepository
import lk.nenasayura.app.data.repository.StudentProfileRepository
import lk.nenasayura.app.data.repository.SubjectRepository

data class StudyProgressUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** null until the first load finishes. */
    val report: ProgressReport? = null
)

class StudyProgressViewModel : ViewModel() {

    private val profileRepository = StudentProfileRepository()
    private val attemptRepository = QuizAttemptRepository()
    private val quizRepository = QuizRepository()
    private val subjectRepository = SubjectRepository()

    private val _uiState = MutableStateFlow(StudyProgressUiState())
    val uiState: StateFlow<StudyProgressUiState> = _uiState.asStateFlow()

    /** Called when the screen appears and every time it comes back (for example after a quiz). */
    fun load() {
        viewModelScope.launch {
            // The spinner shows only while there is nothing to show yet
            _uiState.update { it.copy(isLoading = it.report == null, errorMessage = null) }
            try {
                val profile = profileRepository.getProfile()

                val report = coroutineScope {
                    val attemptsJob = async { attemptRepository.getAttempts(profile.uid) }
                    val quizzesJob = async { quizRepository.getQuizzes() }
                    val subjectsJob = async { subjectRepository.getSubjects() }

                    ProgressAnalyzer.analyze(
                        attempts = attemptsJob.await(),
                        quizzes = quizzesJob.await(),
                        subjects = subjectsJob.await(),
                        mySubjectIds = profile.subjectIds
                    )
                }
                _uiState.update { it.copy(isLoading = false, report = report) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load your progress. Check your internet connection and try again."
                    )
                }
            }
        }
    }
}