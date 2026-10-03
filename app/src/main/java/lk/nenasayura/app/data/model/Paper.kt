package lk.nenasayura.app.data.model



/** A past / model / provincial / target paper stored in Firestore. */
data class Paper(
    val id: String = "",
    val title: String = "",
    val subjectId: String = "",
    val year: Int = 0,
    val type: String = "",
    val fileUrl: String = ""
)

/** Human-friendly name for the stored paper type value. */
fun paperTypeLabel(type: String): String = when (type.lowercase()) {
    "past" -> "Past Paper"
    "model" -> "Model Paper"
    "provincial" -> "Provincial Paper"
    "target" -> "Target Paper"
    else -> type.replaceFirstChar { it.uppercase() }
}