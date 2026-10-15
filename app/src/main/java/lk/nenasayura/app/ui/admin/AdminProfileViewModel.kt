package lk.nenasayura.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.util.Validators

data class AdminProfileUiState(
    val name: String = "",
    val email: String = "",
    /** When the account was created (milliseconds), 0 when unknown. */
    val memberSince: Long = 0L,
    val busy: Boolean = false,
    /** False when the admin signs in with Google only (then there is no app password to change). */
    val hasPasswordLogin: Boolean = true
)

/** The signed-in admin's own profile: show it, change the name, send a password reset link. */
class AdminProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(
        AdminProfileUiState(
            name = auth.currentUser?.displayName.orEmpty(),
            email = auth.currentUser?.email.orEmpty(),
            memberSince = auth.currentUser?.metadata?.creationTimestamp ?: 0L,
            hasPasswordLogin = auth.currentUser?.providerData
                ?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: true
        )
    )
    val state: StateFlow<AdminProfileUiState> = _state.asStateFlow()

    init {
        refreshAccount()
    }

    /**
     * Loads the name and picks up an email change the admin confirmed from the verification link.
     * Called when the profile opens and every time the app comes back to the screen.
     */
    fun refreshAccount() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // Reload so a freshly confirmed email shows up
                try {
                    user.reload().await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Account reload failed: ${e.localizedMessage}")
                }
                val authEmail = auth.currentUser?.email.orEmpty()
                if (authEmail.isNotBlank()) _state.update { it.copy(email = authEmail) }

                val ref = db.collection("users").document(user.uid)
                val doc = ref.get().await()
                val name = doc.getString("name").orEmpty()
                if (name.isNotBlank()) _state.update { it.copy(name = name) }

                // Keep the email saved in Firestore (shown in the staff list) in step with the login email
                val savedEmail = doc.getString("email").orEmpty()
                if (authEmail.isNotBlank() && !savedEmail.equals(authEmail, ignoreCase = true)) {
                    try {
                        ref.update("email", authEmail).await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Saved email update failed: ${e.localizedMessage}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile load failed: ${e.localizedMessage}")
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

    /**
     * Starts an email change. Firebase does not allow switching the login email without proof that the
     * admin owns the new address, so a verification link is sent to it. The email changes when the link
     * is opened. The current password is checked first. [onResult] gets null on success, or an error message.
     */
    fun changeEmail(newEmail: String, currentPassword: String, onResult: (String?) -> Unit) {
        val user = auth.currentUser ?: return onResult("You are signed out.")
        val oldEmail = user.email
        if (oldEmail.isNullOrBlank()) return onResult("This account has no email address.")
        val email = newEmail.trim()
        if (!Validators.isValidEmail(email)) return onResult("Enter a valid email address.")
        if (email.equals(oldEmail, ignoreCase = true)) return onResult("That is already your email.")
        if (currentPassword.isBlank()) return onResult("Enter your current password.")

        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(oldEmail, currentPassword)).await()
                user.verifyBeforeUpdateEmail(email).await()
                _state.update { it.copy(busy = false) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthUserCollisionException) {
                _state.update { it.copy(busy = false) }
                onResult("Another account already uses that email.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _state.update { it.copy(busy = false) }
                onResult("Your current password is wrong.")
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    /**
     * Changes the password right here, no email link needed.
     * Firebase wants a fresh login for this, so the current password is checked first.
     * [onResult] gets null on success, or an error message.
     */
    fun changePassword(currentPassword: String, newPassword: String, onResult: (String?) -> Unit) {
        val user = auth.currentUser ?: return onResult("You are signed out.")
        val email = user.email
        if (email.isNullOrBlank()) return onResult("This account has no email address.")
        if (currentPassword.isBlank()) return onResult("Enter your current password.")
        if (!Validators.isValidPassword(newPassword)) return onResult("New password must be at least 6 characters.")
        if (newPassword == currentPassword) return onResult("Choose a password different from the current one.")

        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
                user.updatePassword(newPassword).await()
                _state.update { it.copy(busy = false) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthWeakPasswordException) {
                // Must come before InvalidCredentials: it is a subclass of it
                _state.update { it.copy(busy = false) }
                onResult("That password is too weak. Try a longer one.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _state.update { it.copy(busy = false) }
                onResult("Your current password is wrong.")
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