package lk.nenasayura.app.core.session



/** Roles in the Nena Sayura app. */
enum class UserRole { ADMIN, TEACHER, STUDENT }

/**
 * The state the Guest and Student modules read to decide what to show.
 * It is provided by the authentication layer (not by the Student module).
 */
sealed interface UserSession {
    /** Nobody is logged in. */
    data object Guest : UserSession

    /** A user is logged in. */
    data class LoggedIn(
        val uid: String,
        val name: String,
        val email: String,
        val role: UserRole
    ) : UserSession
}

/** True only for a logged-in user whose role is Student. */
val UserSession.isStudent: Boolean
    get() = this is UserSession.LoggedIn && role == UserRole.STUDENT
/** True only for a logged-in user whose role is Teacher. */
val UserSession.isTeacher: Boolean
    get() = this is UserSession.LoggedIn && role == UserRole.TEACHER