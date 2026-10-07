package lk.nenasayura.app.core.session



import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

/**
 * Integration point with the authentication module.
 *
 * The authentication teammate implements this interface (for example with
 * Firebase Auth) and exposes the current user as a [UserSession].
 * The Guest and Student modules only ever depend on this interface.
 */
interface SessionProvider {
    /** Emits [UserSession.Guest] when logged out, [UserSession.LoggedIn] otherwise. */
    val session: StateFlow<UserSession>

    /** Logs the user out. Implemented by the authentication layer. */
    fun signOut()
}

/**
 * Holds the active [SessionProvider].
 * When authentication is ready, set `AppSession.provider = <real provider>`
 * once at app start (for example in MainActivity.onCreate).
 */
object AppSession {
    var provider: SessionProvider = DevSessionProvider
}

/** Use inside any composable: `val session by currentSession()`. */
@Composable
fun currentSession(): State<UserSession> =
    AppSession.provider.session.collectAsStateWithLifecycle()