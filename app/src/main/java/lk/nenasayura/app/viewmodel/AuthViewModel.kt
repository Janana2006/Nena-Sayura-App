package lk.nenasayura.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lk.nenasayura.app.data.model.User
import lk.nenasayura.app.data.repository.AuthRepository
import lk.nenasayura.app.util.Validators

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun isLoggedIn(): Boolean = repo.isLoggedIn()
    fun getCurrentUser(): User? = repo.getCurrentUser()

    fun loadProfile(onResult: (Result<User?>) -> Unit) = repo.getProfile(onResult)

    // Login una passe yanna oni screen eka decide karanawa
    fun checkDestination(onResult: (Result<String>) -> Unit) {
        repo.getProfile { result ->
            result.fold(
                onSuccess = { user ->
                    val route = when {
                        user == null -> "Login"
                        user.role.isBlank() -> "RoleSelect"          // CHANGED: Google aluth user, Student da Teacher da ahanna
                        user.role == "teacher" && !user.setupDone -> "TeacherSetup"
                        user.role == "teacher" -> "TeacherHome"
                        !user.setupDone -> "StudentSetup"
                        else -> "Home"
                    }
                    onResult(Result.success(route))
                },
                onFailure = { onResult(Result.failure(it)) }
            )
        }
    }

    fun login(email: String, password: String) {
        if (!Validators.isValidEmail(email)) return showError("Enter a valid email")
        if (password.isEmpty()) return showError("Enter your password")

        _state.value = AuthUiState.Loading
        repo.login(email.trim(), password) { handle(it) }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirm: String,
        role: String,
        isFree: Boolean,
        feeText: String
    ) {
        if (name.isBlank()) return showError("Enter your name")
        if (!Validators.isValidEmail(email)) return showError("Enter a valid email")
        if (!Validators.isValidPassword(password)) return showError("Password must be at least 6 characters")
        if (password != confirm) return showError("Passwords do not match")

        // Teacher kenek Paid select kalama fee eka check karanna
        val free = role != "teacher" || isFree
        var fee = 0
        if (!free) {
            fee = feeText.trim().toIntOrNull() ?: 0
            if (fee <= 0) return showError("Enter a valid fee amount")
        }

        _state.value = AuthUiState.Loading
        repo.register(name.trim(), email.trim(), password, role, free, fee) { handle(it) }
    }
GIT
    fun saveRole(
        role: String,
        isFree: Boolean,
        feeText: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val free = role != "teacher" || isFree
        var fee = 0
        if (!free) {
            fee = feeText.trim().toIntOrNull() ?: 0
            if (fee <= 0) {
                onResult(Result.failure(Exception("Enter a valid fee amount")))
                return
            }
        }
        repo.saveRole(role, free, fee, onResult)
    }

    fun sendResetEmail(email: String) {
        if (!Validators.isValidEmail(email)) return showError("Enter a valid email")

        _state.value = AuthUiState.Loading
        repo.sendResetEmail(email.trim()) { handle(it) }
    }

    // Reset link eke code eka check karanawa
    fun verifyResetCode(code: String, onResult: (Result<String>) -> Unit) =
        repo.verifyResetCode(code, onResult)

    // New password + confirm password check karala save karanawa
    fun confirmPasswordReset(
        code: String,
        password: String,
        confirm: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (!Validators.isValidPassword(password)) {
            return onResult(Result.failure(Exception("Password must be at least 6 characters")))
        }
        if (password != confirm) {
            return onResult(Result.failure(Exception("Passwords do not match")))
        }
        repo.confirmPasswordReset(code, password) { result ->
            result.fold(
                onSuccess = { onResult(Result.success(Unit)) },
                onFailure = { onResult(Result.failure(Exception(resetMessage(it)))) }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        _state.value = AuthUiState.Loading
        repo.loginWithGoogle(idToken) { handle(it) }
    }

    fun logout() = repo.logout()

    fun showError(message: String) {
        _state.value = AuthUiState.Error(message)
    }

    fun resetState() {
        _state.value = AuthUiState.Idle
    }

    private fun handle(result: Result<Unit>) {
        result.fold(
            onSuccess = { _state.value = AuthUiState.Success },
            onFailure = { _state.value = AuthUiState.Error(friendlyMessage(it)) }
        )
    }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthInvalidUserException -> "No account found with this email"
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseAuthUserCollisionException -> "This email is already registered. Please login instead."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later"
        is FirebaseNetworkException -> "No internet connection"
        else -> e.localizedMessage ?: "Something went wrong"
    }

    // Reset password error walata hithata hithawath message
    private fun resetMessage(e: Throwable): String = when (e) {
        is FirebaseAuthActionCodeException -> "This link has expired or was already used"
        is FirebaseAuthWeakPasswordException -> "Choose a stronger password"
        is FirebaseNetworkException -> "No internet connection"
        else -> e.localizedMessage ?: "Something went wrong"
    }
}