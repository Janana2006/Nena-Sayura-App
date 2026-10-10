package lk.nenasayura.app.data.model

/** One account shown in the Admin dashboard (student, teacher or admin staff). */
data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "student",
    val isFree: Boolean = true,
    val fee: Int = 0,
    val streamName: String = "",
    val subjectNames: List<String> = emptyList()
) {
    val isStudent: Boolean get() = role == "student"
    val isTeacher: Boolean get() = role == "teacher"
    val isAdmin: Boolean get() = role == "admin"
}