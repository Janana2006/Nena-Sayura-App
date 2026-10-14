package lk.nenasayura.app.data.model



/** A past / model / provincial / target paper stored in Firestore. */
data class Paper(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val year: Int = 0,
    val type: String = "",
    val fileUrl: String = "",
    /** 1, 2 or 3 for Provincial papers; 0 when the paper has no term. */
    val term: Int = 0,
    /** Province name for Provincial papers (one of [PROVINCES]); empty when not set. */

)

/** "1st Term", "2nd Term", "3rd Term", or an empty string when there is no term. */
fun termLabel(term: Int): String = when (term) {
    1 -> "1st Term"
    2 -> "2nd Term"
    3 -> "3rd Term"
    else -> ""
}

/** Human-friendly name for the stored paper type value. */
fun paperTypeLabel(type: String): String = when (type.lowercase()) {
    "past" -> "Past Paper"
    "model" -> "Model Paper"
    "provincial" -> "Provincial Paper"
    "target" -> "Target Paper"
    else -> type.replaceFirstChar { it.uppercase() }
}


/** True when this paper belongs to [category] ("past", "model" or "provincial"). A paper with no type counts as past. */
fun Paper.isInCategory(category: String): Boolean {
    val paperType = type.trim().lowercase().ifEmpty { "past" }
    return paperType == category.lowercase()
}

/** Screen title for a paper category. */
fun paperCategoryTitle(category: String): String = when (category.lowercase()) {
    "past" -> "Past Papers"
    "model" -> "Model Papers"
    "provincial" -> "Provincial Papers"
    else -> "Papers"
}