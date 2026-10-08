package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.TeacherProfile

/** Reads and updates the teacher's profile in the Firestore "users" collection. */
class TeacherProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getProfile(uid: String, fallbackName: String, fallbackEmail: String): TeacherProfile {
        val doc = db.collection(USERS).document(uid).get().await()
        if (!doc.exists()) {
            return TeacherProfile(uid = uid, name = fallbackName, email = fallbackEmail)
        }

        fun stringList(key: String): List<String> =
            (doc.get(key) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return TeacherProfile(
            uid = uid,
            name = doc.getString("name")?.ifBlank { null } ?: fallbackName,
            email = doc.getString("email")?.ifBlank { null } ?: fallbackEmail,
            bio = doc.getString("bio") ?: "",
            phone = doc.getString("phone") ?: "",
            streamId = doc.getString("streamId") ?: "",
            streamName = doc.getString("streamName") ?: "",
            subjectIds = stringList("subjectIds"),
            subjectNames = stringList("subjectNames"),
            isFree = doc.getBoolean("isFree") ?: true,
            fee = (doc.getLong("fee") ?: 0L).toInt()
        )
    }

    /** Saves only the fields a teacher may edit; every other field of the document stays as it is. */
    suspend fun updateProfile(profile: TeacherProfile) {
        val data = hashMapOf<String, Any>(
            "name" to profile.name.trim(),
            "bio" to profile.bio.trim(),
            "phone" to profile.phone.trim(),
            "isFree" to profile.isFree,
            "fee" to (if (profile.isFree) 0 else profile.fee),
            "subjectIds" to profile.subjectIds,
            "subjectNames" to profile.subjectNames
        )
        db.collection(USERS).document(profile.uid).set(data, SetOptions.merge()).await()
    }

    private companion object {
        const val USERS = "users"
    }
}