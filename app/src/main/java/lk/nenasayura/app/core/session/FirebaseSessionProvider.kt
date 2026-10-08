package lk.nenasayura.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


object FirebaseSessionProvider : SessionProvider {

    private val _session = MutableStateFlow<UserSession>(UserSession.Guest)
    override val session: StateFlow<UserSession> = _session.asStateFlow()

    fun signInStudent(uid: String, name: String, email: String) {
        _session.value = UserSession.LoggedIn(
            uid = uid,
            name = name,
            email = email,
            role = UserRole.STUDENT
        )
    }

    // Teacher login una passe session eka set karanawa (Teacher Home ekata yanna)
    fun signInTeacher(uid: String, name: String, email: String) {
        _session.value = UserSession.LoggedIn(
            uid = uid,
            name = name,
            email = email,
            role = UserRole.TEACHER
        )
    }

    override fun signOut() {
        _session.value = UserSession.Guest
    }
}