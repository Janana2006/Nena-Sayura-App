package lk.nenasayura.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.AdminQuiz
import lk.nenasayura.app.data.model.NewQuestion
import lk.nenasayura.app.data.model.QuizQuestion
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.QuizAdminRepository

const val QUIZ_OPTION_COUNT = 5

data class QuizAdminUiState(
    val loading: Boolean = false,
    val quizzes: List<AdminQuiz> = emptyList(),
    val loadError: String? = null,
    val busy: Boolean = false,
    /** Questions of the quiz the admin is looking at. */
    val questions: List<QuizQuestion> = emptyList(),
    val questionsLoading: Boolean = false,
    val questionsError: String? = null
)

class QuizAdminViewModel(
    private val repo: QuizAdminRepository = QuizAdminRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(QuizAdminUiState())
    val state: StateFlow<QuizAdminUiState> = _state.asStateFlow()

    private var loaded = false

    fun loadOnce() {
        if (loaded) return
        loaded = true
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            try {
                val list = repo.getQuizzes()
                _state.update { it.copy(loading = false, quizzes = list) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = e.localizedMessage ?: "Failed to load quizzes") }
            }
        }
    }

    /** Loads the questions of one quiz so the admin can read them. */
    fun loadQuestions(quizId: String) {
        viewModelScope.launch {
            _state.update { it.copy(questionsLoading = true, questionsError = null) }
            try {
                val list = repo.getQuestions(quizId)
                _state.update { it.copy(questionsLoading = false, questions = list) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        questionsLoading = false,
                        questionsError = e.localizedMessage ?: "Failed to load questions"
                    )
                }
            }
        }
    }

    fun clearQuestions() {
        _state.update { it.copy(questions = emptyList(), questionsLoading = false, questionsError = null) }
    }

    fun createQuiz(
        subject: Subject?,
        title: String,
        description: String,
        questions: List<NewQuestion>,
        onResult: (String?) -> Unit
    ) {
        if (subject == null) return onResult("Please select a subject.")
        if (title.isBlank()) return onResult("Enter a lesson name.")
        if (questions.isEmpty()) return onResult("Add at least one question.")

        val err = validateQuestions(questions)
        if (err != null) return onResult(err)

        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repo.createQuiz(subject.id, title, description, questions)
                val updated = repo.getQuizzes()
                _state.update { it.copy(busy = false, quizzes = updated) }
                onResult(null)
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Failed to save quiz.")
            }
        }
    }

    fun addQuestions(
        quiz: AdminQuiz,
        questions: List<NewQuestion>,
        onResult: (String?) -> Unit
    ) {
        if (questions.isEmpty()) return onResult("Add at least one question.")
        val err = validateQuestions(questions)
        if (err != null) return onResult(err)

        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repo.addQuestions(quiz.id, questions)
                val updated = repo.getQuizzes()
                _state.update { it.copy(busy = false, quizzes = updated) }
                onResult(null)
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Failed to add questions.")
            }
        }
    }

    fun deleteQuiz(quiz: AdminQuiz, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                repo.deleteQuiz(quiz.id)
                val updated = repo.getQuizzes()
                _state.update { it.copy(busy = false, quizzes = updated) }
                onResult(null)
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Failed to delete quiz.")
            }
        }
    }

    private fun validateQuestions(questions: List<NewQuestion>): String? {
        questions.forEachIndexed { i, q ->
            val n = i + 1
            if (q.text.isBlank()) return "Question $n: text cannot be empty."
            if (q.options.size != QUIZ_OPTION_COUNT || q.options.any { it.isBlank() }) {
                return "Question $n: fill all $QUIZ_OPTION_COUNT answers."
            }
            if (q.correctIndex !in q.options.indices) {
                return "Question $n: tap the tick next to the correct answer."
            }
        }
        return null
    }
}