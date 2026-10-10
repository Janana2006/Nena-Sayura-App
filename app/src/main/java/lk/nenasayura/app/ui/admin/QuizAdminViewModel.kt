package lk.nenasayura.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.AdminQuiz
import lk.nenasayura.app.data.model.NewQuestion
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.QuizAdminRepository

/** Every question has this many answers. */
const val QUIZ_OPTION_COUNT = 5

data class QuizAdminUiState(
    val loading: Boolean = true,
    val quizzes: List<AdminQuiz> = emptyList(),
    val loadError: String? = null,
    /** True while a quiz is being saved or deleted. */
    val busy: Boolean = false
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
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val list = repo.getQuizzes()
                _state.update { it.copy(loading = false, quizzes = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Quizzes load failed: ${e.localizedMessage}")
                _state.update {
                    it.copy(loading = false, loadError = "Could not load quizzes: ${e.localizedMessage}")
                }
            }
        }
    }

    /** [onResult] gets null on success, or a message to show on the screen. */
    fun createQuiz(
        subject: Subject?,
        title: String,
        description: String,
        questions: List<NewQuestion>,
        onResult: (String?) -> Unit
    ) {
        val error = when {
            subject == null -> "Choose a stream and a subject."
            title.isBlank() -> "Type the lesson name."
            else -> checkQuestions(questions)
        }
        if (error != null || subject == null) {
            onResult(error)
            return
        }
        act(onResult) { repo.createQuiz(title, subject.id, description, questions) }
    }

    fun addQuestions(quiz: AdminQuiz, questions: List<NewQuestion>, onResult: (String?) -> Unit) {
        val error = checkQuestions(questions)
        if (error != null) {
            onResult(error)
            return
        }
        act(onResult) { repo.addQuestions(quiz.id, questions) }
    }

    fun deleteQuiz(quiz: AdminQuiz, onResult: (String?) -> Unit) {
        act(onResult) { repo.deleteQuiz(quiz.id) }
    }

    private fun checkQuestions(questions: List<NewQuestion>): String? {
        if (questions.isEmpty()) return "Add at least one question."
        questions.forEachIndexed { i, q ->
            val n = i + 1
            if (q.text.isBlank()) return "Question $n: type the question."
            if (q.options.size != QUIZ_OPTION_COUNT || q.options.any { it.isBlank() }) {
                return "Question $n: fill all $QUIZ_OPTION_COUNT answers."
            }
            if (q.correctIndex !in q.options.indices) return "Question $n: tick the correct answer."
        }
        return null
    }

    private fun act(onResult: (String?) -> Unit, block: suspend () -> Unit) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
                // Refresh the list; if only this refresh fails the save itself still worked
                val list = try {
                    repo.getQuizzes()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                _state.update { it.copy(busy = false, quizzes = list ?: it.quizzes) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    private companion object {
        const val TAG = "QuizAdminVM"
    }
}