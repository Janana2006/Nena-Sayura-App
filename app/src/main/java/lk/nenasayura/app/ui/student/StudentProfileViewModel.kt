package lk.nenasayura.app.ui.student.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.session.FirebaseSessionProvider
import lk.nenasayura.app.data.model.StudentProfile
import lk.nenasayura.app.data.repository.StudentProfileRepository
import lk.nenasayura.app.util.Validators

data class StudentProfileUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val profile: StudentProfile? = null,
    val saving: Boolean = false,
    val message: String? = null
)

class StudentProfileViewModel(
    private val repository: StudentProfileRepository = StudentProfileRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(StudentProfileUiState())
    val state: StateFlow<StudentProfileUiState> = _state.asStateFlow()

    private val phoneRegex = Regex("^\\+?[0-9 ]{9,15}$")

    init {
        load()
    }

    fun load(message: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            try {
                val profile = repository.getProfile()
                _state.update {
                    it.copy(loading = false, profile = profile, message = message ?: it.message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        errorMessage = "Could not load your details. Check your internet connection."
                    )
                }
            }
        }
    }

    fun saveDetails(
        name: String,
        phone: String,
        school: String,
        district: String,
        examYear: Int,
        onDone: () -> Unit
    ) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanSchool = school.trim()

        if (cleanName.isEmpty()) return toast("Name cannot be empty")
        if (cleanPhone.isNotEmpty() && !phoneRegex.matches(cleanPhone)) {
            return toast("Enter a valid phone number")
        }

        perform("Profile updated", onDone) {
            repository.updateDetails(cleanName, cleanPhone, cleanSchool, district, examYear)
            val current = _state.value.profile
            _state.update {
                it.copy(
                    profile = current?.copy(
                        name = cleanName,
                        phone = cleanPhone,
                        school = cleanSchool,
                        district = district,
                        examYear = examYear
                    )
                )
            }
            // Keep the rest of the app (Home header) in step with the new name
            if (current != null) {
                FirebaseSessionProvider.signInStudent(current.uid, cleanName, current.email)
            }
        }
    }

    fun changePassword(current: String, newPassword: String, confirm: String, onDone: () -> Unit) {
        if (current.isEmpty()) return toast("Enter your current password")
        if (!Validators.isValidPassword(newPassword)) {
            return toast("New password must be at least 6 characters")
        }
        if (newPassword != confirm) return toast("New passwords do not match")
        if (newPassword == current) return toast("Choose a password different from the current one")

        perform("Password updated", onDone) {
            repository.changePassword(current, newPassword)
        }
    }

    fun changeEmail(currentPassword: String, newEmail: String, onDone: () -> Unit) {
        val email = newEmail.trim()
        if (!Validators.isValidEmail(email)) return toast("Enter a valid email address")
        if (email.equals(_state.value.profile?.email, ignoreCase = true)) {
            return toast("That is already your email address")
        }
        if (currentPassword.isEmpty()) return toast("Enter your current password")

        perform("Verification link sent to $email. Open it, then sign in with the new email.", onDone) {
            repository.changeEmail(currentPassword, email)
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) return toast("No email address found for this account")
        perform("Password reset link sent to $email") {
            repository.sendPasswordReset(email)
        }
    }

    fun showMessage(text: String) = toast(text)

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun toast(text: String) {
        _state.update { it.copy(message = text) }
    }

    // Runs one action with the "saving" spinner, then shows a success or error message
    private fun perform(
        successMessage: String,
        onDone: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                block()
                _state.update { it.copy(saving = false, message = successMessage) }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, message = friendly(e)) }
            }
        }
    }

    // Weak password must be checked before invalid credentials (it is a subclass of it)
    private fun friendly(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password"
        is FirebaseAuthInvalidCredentialsException -> "Your current password is incorrect"
        is FirebaseAuthInvalidUserException -> "Your session has expired. Please sign in again"
        is FirebaseAuthUserCollisionException -> "This email is already used by another account"
        is FirebaseAuthRecentLoginRequiredException -> "Please sign in again and try once more"
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later"
        is FirebaseNetworkException -> "No internet connection"
        else -> e.localizedMessage ?: "Something went wrong"
    }
}