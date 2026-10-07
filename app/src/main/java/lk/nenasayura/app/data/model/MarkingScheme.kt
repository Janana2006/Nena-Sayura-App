package lk.nenasayura.app.data.model

/** A marking scheme (answer guide) for an A/L paper, stored in Firestore. */
data class MarkingScheme(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val year: Int = 0,
    /** Which kind of paper it belongs to: "past", "model" or "provincial" (optional in the data). */
    val type: String = "",
    val fileUrl: String = ""
)

/** True when this scheme belongs to [category] ("past", "model" or "provincial"). A scheme with no type counts as past. */
fun MarkingScheme.isInCategory(category: String): Boolean {
    val schemeType = type.trim().lowercase().ifEmpty { "past" }
    return schemeType == category.lowercase()
}

/** Screen title for a marking scheme category. */
fun markingSchemeCategoryTitle(category: String): String = when (category.lowercase()) {
    "past" -> "Past Paper Schemes"
    "model" -> "Model Paper Schemes"
    "provincial" -> "Provincial Paper Schemes"
    else -> "Marking Schemes"
}