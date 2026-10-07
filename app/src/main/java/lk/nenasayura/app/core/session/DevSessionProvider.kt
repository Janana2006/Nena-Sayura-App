package lk.nenasayura.app.core.session



import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TEMPORARY stub used only while the authentication module is unfinished.
 * It does NOT authenticate anyone; it just lets us test Student screens.
 * Delete this file when the real SessionProvider is plugged in.
 */
object DevSessionProvider : SessionProvider {

    private val _session = MutableStateFlow<UserSession>(UserSession.Guest)
    override val session: StateFlow<UserSession> = _session.asStateFlow()

    fun signInAsTestStudent() {
        _session.value = UserSession.LoggedIn(
            uid = "dev-student-001",
            name = "Test Student",
            email = "student@test.local",
            role = UserRole.STUDENT
        )
    }

    override fun signOut() {
        _session.value = UserSession.Guest
    }
}