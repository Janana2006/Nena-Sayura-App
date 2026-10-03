package lk.nenasayura.app.data.model



/** A marking scheme (answer guide) for an A/L paper, stored in Firestore. */
data class MarkingScheme(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val year: Int = 0,
    val fileUrl: String = ""
)