package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
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
        return doc.toTeacherProfile(fallbackName, fallbackEmail)
    }

    /**
     * Every teacher account, for the student's "My teacher" list.
     * Teachers are found two ways: role = "teacher" (active role) and roles contains "teacher"
     * (a student who also added the teacher role, so their active role may be "student").
     */
    suspend fun getAllTeachers(): List<TeacherProfile> {
        val users = db.collection(USERS)
        val byRole = users.whereEqualTo("role", "teacher").get().await().documents
        val byRoles = try {
            users.whereArrayContains("roles", "teacher").get().await().documents
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        return (byRole + byRoles)
            .distinctBy { it.id }
            .map { it.toTeacherProfile("", "") }
    }

    private fun DocumentSnapshot.toTeacherProfile(fallbackName: String, fallbackEmail: String): TeacherProfile {
        val doc = this

        // A student who later added the teacher role keeps the teaching subject in "teacherStudy"
        @Suppress("UNCHECKED_CAST")
        val study = doc.get("teacherStudy") as? Map<String, Any?>

        fun stringList(key: String): List<String> =
            ((study?.get(key) ?: doc.get(key)) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return TeacherProfile(
            uid = doc.id,
            name = doc.getString("name")?.ifBlank { null } ?: fallbackName,
            email = doc.getString("email")?.ifBlank { null } ?: fallbackEmail,
            bio = doc.getString("bio") ?: "",
            phone = doc.getString("phone") ?: "",
            streamId = (study?.get("streamId") ?: doc.getString("streamId")) as? String ?: "",
            streamName = (study?.get("streamName") ?: doc.getString("streamName")) as? String ?: "",
            subjectIds = stringList("subjectIds"),
            subjectNames = stringList("subjectNames"),
            isFree = doc.getBoolean("isFree") ?: true,
            fee = (doc.getLong("fee") ?: 0L).toInt()
        )
    }

    /** Saves only the fields a teacher may edit; every other field of the document stays as it is. */
    suspend fun updateProfile(profile: TeacherProfile) {
        val ref = db.collection(USERS).document(profile.uid)
        val hasOwnStudy = ref.get().await().get("teacherStudy") is Map<*, *>

        val data = hashMapOf<String, Any>(
            "name" to profile.name.trim(),
            "bio" to profile.bio.trim(),
            "phone" to profile.phone.trim(),
            "isFree" to profile.isFree,
            "fee" to (if (profile.isFree) 0 else profile.fee)
        )
        if (hasOwnStudy) {
            // Student + Teacher account: do not touch the student's top-level subjects
            data["teacherStudy"] = hashMapOf(
                "subjectIds" to profile.subjectIds,
                "subjectNames" to profile.subjectNames
            )
        } else {
            data["subjectIds"] = profile.subjectIds
            data["subjectNames"] = profile.subjectNames
        }
        ref.set(data, SetOptions.merge()).await()
    }

    private companion object {
        const val USERS = "users"
    }
}