package lk.nenasayura.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions   // NEW
import lk.nenasayura.app.data.model.User

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUser(): User? {
        val user = auth.currentUser ?: return null
        return User(
            uid = user.uid,
            name = user.displayName ?: "",
            email = user.email ?: ""
        )
    }

    // Firestore eken profile eka read karanawa (role, setupDone, stream, subjects)
    fun getProfile(onResult: (Result<User?>) -> Unit) {
        val fbUser = auth.currentUser
        if (fbUser == null) {
            onResult(Result.success(null))
            return
        }

        db.collection("users").document(fbUser.uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // CHANGED: Google login wage profile nathi ayata role danne naha,
                    // RoleSelect page eka penna role = "" (blank) karanawa
                    onResult(
                        Result.success(
                            User(
                                uid = fbUser.uid,
                                name = fbUser.displayName ?: "",
                                email = fbUser.email ?: "",
                                role = "",
                                setupDone = false
                            )
                        )
                    )
                    return@addOnSuccessListener
                }

                val subjectIds = (doc.get("subjectIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val subjectNames = (doc.get("subjectNames") as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                onResult(
                    Result.success(
                        User(
                            uid = fbUser.uid,
                            name = doc.getString("name") ?: fbUser.displayName ?: "",
                            email = doc.getString("email") ?: fbUser.email ?: "",
                            role = doc.getString("role") ?: "student",
                            isFree = doc.getBoolean("isFree") ?: true,
                            fee = (doc.getLong("fee") ?: 0L).toInt(),
                            setupDone = doc.getBoolean("setupDone") ?: true,
                            streamId = doc.getString("streamId") ?: "",
                            streamName = doc.getString("streamName") ?: "",
                            subjectIds = subjectIds,
                            subjectNames = subjectNames
                        )
                    )
                )
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    recordLoginMethod("email")
                    onResult(Result.success(Unit))
                    return@addOnCompleteListener
                }

                val error = task.exception
                // The Register screen removes spaces from passwords, so the saved password has none.
                // If the typed password has a space (or the keyboard added one at the end), try again without spaces.
                val withoutSpaces = password.filter { !it.isWhitespace() }
                if (error is FirebaseAuthInvalidCredentialsException &&
                    withoutSpaces.isNotEmpty() && withoutSpaces != password
                ) {
                    auth.signInWithEmailAndPassword(email, withoutSpaces)
                        .addOnCompleteListener { retry ->
                            if (retry.isSuccessful) {
                                recordLoginMethod("email")
                                onResult(Result.success(Unit))
                            } else onResult(Result.failure(retry.exception ?: error))
                        }
                } else {
                    onResult(Result.failure(error ?: Exception("Login failed")))
                }
            }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        isFree: Boolean,
        fee: Int,
        onResult: (Result<Unit>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(Result.failure(task.exception ?: Exception("Register failed")))
                    return@addOnCompleteListener
                }

                val user = task.result.user
                if (user == null) {
                    onResult(Result.success(Unit))
                    return@addOnCompleteListener
                }

                val profile = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                user.updateProfile(profile).addOnCompleteListener {

                    val data = hashMapOf(
                        "uid" to user.uid,
                        "name" to name,
                        "email" to email,
                        "role" to role,
                        "loginMethod" to "email",
                        "isFree" to isFree,
                        "fee" to fee,
                        "setupDone" to false   // teacher saha student dekatama stream/subject select karanna oni
                    )
                    db.collection("users").document(user.uid).set(data)
                        .addOnCompleteListener { saveTask ->
                            if (!saveTask.isSuccessful) {
                                Log.e("AuthRepository", "Profile save failed", saveTask.exception)
                            }
                            onResult(Result.success(Unit))
                        }
                }
            }
    }

    // NEW: Google login wage aluth user ge role (saha teacher nam Free/Paid + fee) save karanawa
    fun saveRole(role: String, isFree: Boolean, fee: Int, onResult: (Result<Unit>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(Exception("Not logged in")))
            return
        }

        val data = hashMapOf<String, Any>(
            "uid" to user.uid,
            "role" to role,
            "loginMethod" to if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) "google" else "email",
            "isFree" to isFree,
            "fee" to fee,
            "setupDone" to false        // dan stream/subjects select karanna oni
        )
        user.email?.let { data["email"] = it }
        user.displayName?.takeIf { it.isNotBlank() }?.let { data["name"] = it }

        db.collection("users").document(user.uid).set(data, SetOptions.merge())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Failed to save")))
            }
    }

    fun sendResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Failed to send email")))
            }
    }

    // Reset link eke code eka (oobCode) valid da balala, e code eka ekka link wela thiyena email eka denawa
    fun verifyResetCode(code: String, onResult: (Result<String>) -> Unit) {
        auth.verifyPasswordResetCode(code)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(task.result ?: ""))
                else onResult(Result.failure(task.exception ?: Exception("Invalid or expired link")))
            }
    }

    // Code eka valid nam aluth password eka save karanawa
    fun confirmPasswordReset(code: String, newPassword: String, onResult: (Result<Unit>) -> Unit) {
        auth.confirmPasswordReset(code, newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Failed to reset password")))
            }
    }

    fun loginWithGoogle(idToken: String, onResult: (Result<Unit>) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    recordLoginMethod("google")
                    onResult(Result.success(Unit))
                } else onResult(Result.failure(task.exception ?: Exception("Google login failed")))
            }
    }

    /**
     * Saves how this account signs in, so the Admin > Users list can show it.
     * update() only works when the profile already exists, so a brand new Google user
     * is NOT given a half-empty profile here (saveRole writes it later with the role).
     * Old accounts get filled in the next time they log in.
     */
    private fun recordLoginMethod(method: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("loginMethod", method)
            .addOnFailureListener { Log.d("AuthRepository", "loginMethod not saved yet: ${it.message}") }
    }

    fun logout() {
        auth.signOut()
    }
}