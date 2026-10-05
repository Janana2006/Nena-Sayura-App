package lk.nenasayura.app.data.model

/** A guide book or resource book stored in Firestore. */
data class GuideBook(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val author: String = "",
    /** "guide" or "resource" (optional in the data). */
    val category: String = "",
    val fileUrl: String = ""
)

/** True when this book belongs to [category] ("guide" or "resource"). A book with no category counts as a guide book. */
fun GuideBook.isInCategory(category: String): Boolean {
    val bookCategory = this.category.trim().lowercase().ifEmpty { "guide" }
    return bookCategory == category.lowercase()
}

/** Screen title for a guide book category. */
fun guideBookCategoryTitle(category: String): String = when (category.lowercase()) {
    "resource" -> "Resource Books"
    else -> "Guide Books"
}