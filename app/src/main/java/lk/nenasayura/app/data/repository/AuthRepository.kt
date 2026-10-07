package lk.nenasayura.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
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
                    // Google login wage profile nathi ayata: student widihata
                    onResult(
                        Result.success(
                            User(
                                uid = fbUser.uid,
                                name = fbUser.displayName ?: "",
                                email = fbUser.email ?: ""
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
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Login failed")))
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
                        "isFree" to isFree,
                        "fee" to fee,
                        "setupDone" to (role != "teacher")   // teacher nam false, stream/subject select karanna oni
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

    fun sendResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Failed to send email")))
            }
    }

    fun loginWithGoogle(idToken: String, onResult: (Result<Unit>) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Google login failed")))
            }
    }

    fun logout() {
        auth.signOut()
    }
}