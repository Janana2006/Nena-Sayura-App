package lk.nenasayura.app.data.model



/** A guide book (e.g. a teacher guide) stored in Firestore. */
data class GuideBook(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val author: String = "",
    val fileUrl: String = ""
)