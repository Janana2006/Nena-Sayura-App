package lk.nenasayura.app.data.model

data class StudyStream(
    val id: String = "",
    val name: String = "",
    val subjects: List<Subject> = emptyList(),
    val groups: List<List<Subject>> = emptyList()
)