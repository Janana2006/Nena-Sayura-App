package lk.nenasayura.app.data.model

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val role: String = "student",
    val isFree: Boolean = true,
    val fee: Int = 0,
    val setupDone: Boolean = true,
    val streamId: String = "",
    val streamName: String = "",
    val subjectIds: List<String> = emptyList(),
    val subjectNames: List<String> = emptyList()
)