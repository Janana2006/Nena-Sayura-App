package lk.nenasayura.app.ui.student.quizzes

import androidx.lifecycle.SavedStateHandle
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
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.QuizRepository
import lk.nenasayura.app.data.repository.SubjectRepository

/** Everything the quiz list screen needs to draw itself. */
data class QuizListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizzes: List<Quiz> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val query: String = ""
) {
    fun subjectName(subjectId: String): String =
        subjects.firstOrNull { it.id == subjectId }?.name ?: ""

    // Only offer subjects that actually have quizzes
    val availableSubjects: List<Subject>
        get() {
            val usedIds = quizzes.map { it.subjectId }.toSet()
            return subjects.filter { it.id in usedIds }
        }

    val hasActiveFilters: Boolean
        get() = selectedSubjectId != null || query.isNotBlank()

    val filteredQuizzes: List<Quiz>
        get() {
            val text = query.trim()
            return quizzes.filter { quiz ->
                (selectedSubjectId == null || quiz.subjectId == selectedSubjectId) &&
                        (text.isEmpty() ||
                                quiz.title.contains(text, ignoreCase = true) ||
                                subjectName(quiz.subjectId).contains(text, ignoreCase = true))
            }
        }
}

class QuizListViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // NEW: students pick a subject first. Null for guests (they see all quizzes with a filter).
    val subjectId: String? = savedStateHandle.get<String>("subjectId")?.takeIf { it.isNotBlank() }

    private val quizRepository = QuizRepository()
    private val subjectRepository = SubjectRepository()

    private val _uiState = MutableStateFlow(QuizListUiState())
    val uiState: StateFlow<QuizListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (quizzes, subjects) = coroutineScope {
                    val quizzesJob = async { quizRepository.getQuizzes() }
                    val subjectsJob = async { subjectRepository.getSubjects() }
                    quizzesJob.await() to subjectsJob.await()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Students: keep only the chosen subject's quizzes
                        quizzes = quizzes.filter { quiz -> subjectId == null || quiz.subjectId == subjectId },
                        subjects = subjects
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

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onSubjectSelected(subjectId: String?) = _uiState.update { it.copy(selectedSubjectId = subjectId) }

    fun clearFilters() = _uiState.update {
        it.copy(selectedSubjectId = null, query = "")
    }
}