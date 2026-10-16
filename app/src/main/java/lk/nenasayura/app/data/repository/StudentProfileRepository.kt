package lk.nenasayura.app.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.StudentProfile

/** Reads and updates the logged-in student's profile and sign-in details. */
class StudentProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getProfile(): StudentProfile {
        val first = auth.currentUser ?: throw IllegalStateException("Not logged in")

        // Pick up an email change the student confirmed from the verification link
        try {
            first.reload().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline or expired session: carry on with what we have
        }
        val user = auth.currentUser ?: first

        val ref = db.collection(USERS).document(user.uid)
        val doc = ref.get().await()
        val authEmail = user.email ?: ""

        // Keep the email stored in Firestore in step with the login email
        if (authEmail.isNotBlank() && doc.exists() && doc.getString("email") != authEmail) {
            try {
                ref.set(hashMapOf<String, Any>("email" to authEmail), SetOptions.merge()).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Not critical
            }
        }

        // A teacher who later added the student role keeps the student's stream in "studentStudy"
        @Suppress("UNCHECKED_CAST")
        val study = doc.get("studentStudy") as? Map<String, Any?>

        fun stringList(key: String): List<String> =
            ((study?.get(key) ?: doc.get(key)) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return StudentProfile(
            uid = user.uid,
            name = doc.getString("name")?.ifBlank { null } ?: user.displayName ?: "",
            email = authEmail.ifBlank { doc.getString("email") ?: "" },
            phone = doc.getString("phone") ?: "",
            school = doc.getString("school") ?: "",
            district = doc.getString("district") ?: "",
            examYear = (doc.get("examYear") as? Number)?.toInt() ?: 0,
            streamId = (study?.get("streamId") ?: doc.getString("streamId")) as? String ?: "",
            streamName = (study?.get("streamName") ?: doc.getString("streamName")) as? String ?: "",
            subjectIds = stringList("subjectIds"),
            subjectNames = stringList("subjectNames"),
            isGoogleAccount = user.isGoogle()
        )
    }

    /** Saves the personal details. Every other field of the document stays as it is. */
    suspend fun updateDetails(
        name: String,
        phone: String,
        school: String,
        district: String,
        examYear: Int
    ) {
        val user = auth.currentUser ?: throw IllegalStateException("Not logged in")
        val data = hashMapOf<String, Any>(
            "name" to name.trim(),
            "phone" to phone.trim(),
            "school" to school.trim(),
            "district" to district,
            "examYear" to examYear
        )
        db.collection(USERS).document(user.uid).set(data, SetOptions.merge()).await()

        // Keep the Firebase Auth display name in step too
        val change = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
        user.updateProfile(change).await()
    }

    /** Changes the password directly (no email link). The current password is checked first. */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = requirePasswordAccount()
        reauthenticate(user, currentPassword)
        user.updatePassword(newPassword).await()
    }

    /**
     * Starts an email change. Firebase sends a verification link to the NEW email and the
     * email changes only after the student opens that link.
     */
    suspend fun changeEmail(currentPassword: String, newEmail: String) {
        val user = requirePasswordAccount()
        reauthenticate(user, currentPassword)
        user.verifyBeforeUpdateEmail(newEmail.trim()).await()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    // Google accounts cannot change their email or password here
    private fun requirePasswordAccount(): FirebaseUser {
        val user = auth.currentUser ?: throw IllegalStateException("Not logged in")
        if (user.isGoogle()) {
            throw IllegalStateException("Your email and password are managed by your Google account")
        }
        return user
    }

    private suspend fun reauthenticate(user: FirebaseUser, password: String) {
        val email = user.email ?: throw IllegalStateException("This account has no email address")
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
    }

    private fun FirebaseUser.isGoogle(): Boolean =
        providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

    private companion object {
        const val USERS = "users"
    }
}