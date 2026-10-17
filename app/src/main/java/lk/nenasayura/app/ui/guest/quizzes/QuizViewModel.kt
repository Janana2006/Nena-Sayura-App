package lk.nenasayura.app.ui.guest.quizzes



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
import lk.nenasayura.app.core.session.AppSession
import lk.nenasayura.app.core.session.UserSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.data.model.AttemptAnswer
import lk.nenasayura.app.data.model.QuizAttempt
import lk.nenasayura.app.data.model.QuizQuestion
import lk.nenasayura.app.data.repository.QuizAttemptRepository
import lk.nenasayura.app.data.repository.QuizRepository

/** State of one quiz attempt: the questions, the answers so far, and whether it is finished. */
data class QuizUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quizTitle: String = "",
    val subjectId: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    /** question index -> chosen option index */
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    /** question indexes whose answer has already been checked */
    val checkedQuestions: Set<Int> = emptySet(),
    val isFinished: Boolean = false
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex

    val score: Int
        get() = questions.indices.count { selectedAnswers[it] == questions[it].correctIndex }

    val percent: Int
        get() = if (questions.isEmpty()) 0 else score * 100 / questions.size
}

class QuizViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val quizId: String = savedStateHandle.get<String>("quizId") ?: ""
    private val quizRepository = QuizRepository()
    private val attemptRepository = QuizAttemptRepository()

    /** Stops one finished attempt from being saved twice. */
    private var attemptSaved = false

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (quiz, questions) = coroutineScope {
                    val quizJob = async { quizRepository.getQuiz(quizId) }
                    val questionsJob = async { quizRepository.getQuestions(quizId) }
                    quizJob.await() to questionsJob.await()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        quizTitle = quiz?.title ?: "Quiz",
                        subjectId = quiz?.subjectId ?: "",
                        questions = questions
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load this quiz. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    /** The student taps an option (only allowed before the answer is checked). */
    fun selectAnswer(optionIndex: Int) = _uiState.update { state ->
        if (state.isFinished || state.currentIndex in state.checkedQuestions) {
            state
        } else {
            state.copy(selectedAnswers = state.selectedAnswers + (state.currentIndex to optionIndex))
        }
    }

    /** Reveals whether the selected option is right, with the explanation. */
    fun checkAnswer() = _uiState.update { state ->
        if (state.currentIndex in state.selectedAnswers) {
            state.copy(checkedQuestions = state.checkedQuestions + state.currentIndex)
        } else {
            state
        }
    }

    /** Goes to the next question, or submits the quiz after the last one. */
    fun next() {
        val state = _uiState.value
        if (state.isFinished) return
        if (state.isLastQuestion) {
            _uiState.update { it.copy(isFinished = true) }
            saveAttempt(state)
        } else {
            _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        }
    }

    /**
     * Saves the finished attempt for the Study Progress screen.
     * Only logged-in students are tracked; guests are skipped.
     */
    private fun saveAttempt(state: QuizUiState) {
        if (attemptSaved || state.questions.isEmpty()) return
        val session = AppSession.provider.session.value
        if (!session.isStudent) return
        val uid = (session as UserSession.LoggedIn).uid
        attemptSaved = true

        val attempt = QuizAttempt(
            quizId = quizId,
            quizTitle = state.quizTitle,
            subjectId = state.subjectId,
            score = state.score,
            total = state.questions.size,
            answers = state.questions.mapIndexed { index, question ->
                val selected = state.selectedAnswers[index] ?: -1
                AttemptAnswer(
                    questionId = question.id,
                    selectedIndex = selected,
                    correct = selected == question.correctIndex
                )
            },
            createdAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                attemptRepository.saveAttempt(uid, attempt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Not critical: the quiz result is still shown. Let the next attempt try again.
                attemptSaved = false
            }
        }
    }

    /** Starts the same quiz again from the first question. */
    fun restart() {
        attemptSaved = false
        _uiState.update {
            it.copy(
                currentIndex = 0,
                selectedAnswers = emptyMap(),
                checkedQuestions = emptySet(),
                isFinished = false
            )
        }
    }
}