package lk.nenasayura.app.data.model

data class Subject(
    val id: String = "",
    val name: String = ""
)

data class StudyStream(
    val id: String = "",
    val name: String = "",
    val subjects: List<Subject> = emptyList()
)