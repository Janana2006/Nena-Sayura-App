package lk.nenasayura.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AdminProfileUiState(
    val name: String = "",
    val email: String = "",
    /** When the account was created (milliseconds), 0 when unknown. */
    val memberSince: Long = 0L,
    val busy: Boolean = false
)

/** The signed-in admin's own profile: show it, change the name, send a password reset link. */
class AdminProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(
        AdminProfileUiState(
            name = auth.currentUser?.displayName.orEmpty(),
            email = auth.currentUser?.email.orEmpty(),
            memberSince = auth.currentUser?.metadata?.creationTimestamp ?: 0L
        )
    )
    val state: StateFlow<AdminProfileUiState> = _state.asStateFlow()

    init {
        loadName()
    }

    // The name the admin typed when the account was made lives in the Firestore "users" document
    private fun loadName() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val name = doc.getString("name").orEmpty()
                if (name.isNotBlank()) _state.update { it.copy(name = name) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile name load failed: ${e.localizedMessage}")
            }
        }
    }

    /** [onResult] gets null on success, or an error message. */
    fun updateName(newName: String, onResult: (String?) -> Unit) {
        val name = newName.trim()
        if (name.isBlank()) return onResult("Enter your name.")
        val user = auth.currentUser ?: return onResult("You are signed out.")

        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                db.collection("users").document(user.uid).update("name", name).await()
                // Nice to have: keep the login profile in step. A failure here is not important.
                try {
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Display name update failed: ${e.localizedMessage}")
                }
                _state.update { it.copy(name = name, busy = false) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    /** Sends a "choose a new password" link to the admin's email. */
    fun sendPasswordReset(onResult: (String?) -> Unit) {
        val email = auth.currentUser?.email
        if (email.isNullOrBlank()) return onResult("This account has no email address.")

        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _state.update { it.copy(busy = false) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    private companion object {
        const val TAG = "AdminProfileVM"
    }
}