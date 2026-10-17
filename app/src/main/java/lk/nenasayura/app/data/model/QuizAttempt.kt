package lk.nenasayura.app.data.model



/** The student's answer to one question of an attempt. */
data class AttemptAnswer(
    val questionId: String = "",
    /** Option the student chose (-1 = not answered). */
    val selectedIndex: Int = -1,
    val correct: Boolean = false
)

/**
 * One finished try of a quiz, saved at:  users/{uid}/quiz_attempts/{id}
 * Every try is a new document, so retrying a quiz keeps the history.
 */
data class QuizAttempt(
    val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val subjectId: String = "",
    val score: Int = 0,
    val total: Int = 0,
    val answers: List<AttemptAnswer> = emptyList(),
    val createdAtMillis: Long = 0L
) {
    val percent: Int get() = if (total <= 0) 0 else score * 100 / total
}