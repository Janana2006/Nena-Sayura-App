package lk.nenasayura.app.ui.student.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.browse.QuizBrowseSelection
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.QuizRepository
import lk.nenasayura.app.data.repository.SubjectRepository

data class QuizSubjectUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val streamName: String = "",
    val subjects: List<Subject> = emptyList(),
    // subjectId -> number of quizzes
    val quizCounts: Map<String, Int> = emptyMap()
)

/** Loads the subjects of the chosen stream and how many quizzes each one has. */
class QuizSubjectSelectViewModel : ViewModel() {

    private val subjectRepository = SubjectRepository()
    private val quizRepository = QuizRepository()

    private val _uiState = MutableStateFlow(QuizSubjectUiState())
    val uiState: StateFlow<QuizSubjectUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        // Stream eka wenas unama ("Change") subjects aye load wenawa
        viewModelScope.launch {
            QuizBrowseSelection.state
                .map { it.streamId }
                .distinctUntilChanged()
                .collect { streamId -> load(streamId) }
        }
    }

    fun retry() = load(QuizBrowseSelection.state.value.streamId)

    private fun load(streamId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (streamId.isEmpty()) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Please choose your stream first.")
                }
                return@launch
            }

            try {
                val (stream, quizzes) = coroutineScope {
                    val streamJob = async { subjectRepository.getStream(streamId) }
                    val quizzesJob = async { quizRepository.getQuizzes() }
                    streamJob.await() to quizzesJob.await()
                }

                if (stream == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "This stream could not be found.")
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        streamName = stream.name,
                        subjects = stream.subjects,
                        quizCounts = quizzes.groupingBy { quiz -> quiz.subjectId }.eachCount()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load subjects. Check your internet connection and try again."
                    )
                }
            }
        }
    }
}