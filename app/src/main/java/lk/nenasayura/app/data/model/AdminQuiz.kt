package lk.nenasayura.app.data.model

/** A quiz as the admin sees it in the list: the lesson name, its subject and how many questions it has. */
data class AdminQuiz(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val description: String = "",
    /** -1 when the quiz document has no count saved. */
    val questionCount: Int = -1
)

/** One new multiple-choice question typed by the admin. [correctIndex] is -1 until a tick is chosen. */
data class NewQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)