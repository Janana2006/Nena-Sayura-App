package lk.nenasayura.app.data.model



/** A quiz (a set of MCQs) for one subject. */
data class Quiz(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val description: String = ""
)

/** One multiple-choice question. [correctIndex] is the position of the right option. */
data class QuizQuestion(
    val id: String = "",
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
)