package lk.nenasayura.app.data.model



/** An A/L subject, e.g. "Chemistry" in the "Physical Science" stream. */
data class Subject(
    val id: String = "",
    val name: String = "",
    val stream: String = ""
)