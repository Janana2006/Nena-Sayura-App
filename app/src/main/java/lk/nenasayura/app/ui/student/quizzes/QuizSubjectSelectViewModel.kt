package lk.nenasayura.app.ui.student.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.data.repository.QuizRepository

data class QuizSubjectUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // subjectId -> number of quizzes
    val quizCounts: Map<String, Int> = emptyMap()
)

/**
 * Students: the stream + subjects come from the student's saved profile
 * (loaded by StudentStudySync into PaperBrowseSelection), so only the student's own
 * subjects are shown. This ViewModel only loads how many quizzes each subject has.
 */
class QuizSubjectSelectViewModel : ViewModel() {

    private val quizRepository = QuizRepository()

    private val _uiState = MutableStateFlow(QuizSubjectUiState())
    val uiState: StateFlow<QuizSubjectUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val quizzes = quizRepository.getQuizzes()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        quizCounts = quizzes.groupingBy { quiz -> quiz.subjectId }.eachCount()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load quizzes. Check your internet connection and try again."
                    )
                }
            }
        }
    }
}