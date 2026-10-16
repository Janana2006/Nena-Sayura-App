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
    val subjectNames: List<String> = emptyList(),
    /** Staff only: key of the chosen StaffRole ("" for old accounts). */
    val staffRole: String = "",
    /** Staff only: saved access keys. null = old account = full access. */
    val permissions: List<String>? = null,
    /** How the account signs in: "email" or "google" ("" = old account, not recorded yet). */
    val loginMethod: String = ""
) {
    val isStudent: Boolean get() = role == "student"
    val isTeacher: Boolean get() = role == "teacher"
    val isAdmin: Boolean get() = role == "admin"
    val login: LoginMethod get() = LoginMethod.fromKey(loginMethod)

    /** What this staff member may open in the Admin app. */
    val access: Set<AdminPermission> get() = AdminPermission.fromKeys(permissions)
    val staffRoleLabel: String get() = StaffRole.fromKey(staffRole)?.label ?: "Full Admin"
}

/** Sign-in method shown in the Admin > Users list. */
enum class LoginMethod(val key: String, val label: String) {
    Email("email", "Email"),
    Google("google", "Google"),
    Unknown("", "Not recorded");

    companion object {
        fun fromKey(key: String?): LoginMethod = entries.firstOrNull { it.key == key } ?: Unknown
    }
}