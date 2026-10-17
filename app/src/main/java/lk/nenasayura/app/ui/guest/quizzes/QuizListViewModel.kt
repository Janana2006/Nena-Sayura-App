package lk.nenasayura.app.ui.guest.quizzes

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
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.core.browse.PaperBrowseState
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.QuizRepository
import lk.nenasayura.app.data.repository.SubjectRepository

/** Everything the quiz list screen needs to draw itself. */
data class QuizListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // The quizzes of the chosen subjects (or all quizzes when no stream/subjects were chosen)
    val quizzes: List<Quiz> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val query: String = "",
    // true = the guest chose a stream + subjects: only those subjects, no "All" chip
    val restricted: Boolean = false,
    val streamName: String = ""
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
        get() = (!restricted && selectedSubjectId != null) || query.isNotBlank()

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

class QuizListViewModel(
    private val quizRepository: QuizRepository = QuizRepository(),
    private val subjectRepository: SubjectRepository = SubjectRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizListUiState())
    val uiState: StateFlow<QuizListUiState> = _uiState.asStateFlow()

    // Everything from Firestore. The list on the screen is cut down from these.
    private var allQuizzes: List<Quiz> = emptyList()
    private var allSubjects: List<Subject> = emptyList()
    private var loaded = false

    init {
        load()
        // When the guest presses "Change" and chooses other subjects, update the list
        viewModelScope.launch {
            PaperBrowseSelection.state.collect { selection ->
                if (loaded) applySelection(selection)
            }
        }
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
                allQuizzes = quizzes
                allSubjects = subjects
                loaded = true
                applySelection(PaperBrowseSelection.state.value)
                _uiState.update { it.copy(isLoading = false) }
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

    // With a stream + subjects chosen: only those subjects (the first one that has quizzes is opened).
    // Without: all quizzes, with the "All" chip.
    private fun applySelection(selection: PaperBrowseState) {
        _uiState.update { state ->
            if (selection.hasSelection) {
                val ids = selection.subjectIds
                val quizzes = allQuizzes.filter { it.subjectId in ids }
                val firstWithQuizzes = selection.subjects
                    .firstOrNull { subject -> quizzes.any { it.subjectId == subject.id } }
                state.copy(
                    quizzes = quizzes,
                    subjects = selection.subjects,
                    restricted = true,
                    streamName = selection.streamName,
                    selectedSubjectId = firstWithQuizzes?.id,
                    query = ""
                )
            } else {
                state.copy(
                    quizzes = allQuizzes,
                    subjects = allSubjects,
                    restricted = false,
                    streamName = "",
                    selectedSubjectId = null
                )
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onSubjectSelected(subjectId: String?) = _uiState.update { it.copy(selectedSubjectId = subjectId) }

    fun clearFilters() = _uiState.update {
        // With chosen subjects there is no "All", so the chosen subject stays
        it.copy(
            selectedSubjectId = if (it.restricted) it.selectedSubjectId else null,
            query = ""
        )
    }
}