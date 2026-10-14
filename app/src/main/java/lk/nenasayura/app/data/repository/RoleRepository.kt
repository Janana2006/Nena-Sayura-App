package lk.nenasayura.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject

/**
 * One account can be BOTH a student and a teacher (no second registration).
 *
 * Firestore "users" document:
 *  - role  : the ACTIVE role (the dashboard opened at the next auto-login)
 *  - roles : every role the account has, e.g. ["student", "teacher"]
 *            (missing on old accounts = just [role])
 *  - studentStudy / teacherStudy : stream + subjects of a role that was ADDED later
 *            (the first role keeps using the old top-level streamId / subjectIds fields,
 *             because a teacher picks 1 subject and a student picks 3 - they must not overwrite each other)
 */
class RoleRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    data class RoleInfo(val roles: Set<String>, val active: String) {
        val isStudent: Boolean get() = "student" in roles
        val isTeacher: Boolean get() = "teacher" in roles
    }

    private fun uid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

    suspend fun getInfo(): RoleInfo {
        val doc = db.collection(USERS).document(uid()).get().await()
        val active = doc.getString("role")?.ifBlank { null } ?: "student"
        val roles = (doc.get("roles") as? List<*>)?.filterIsInstance<String>()?.toMutableSet()
            ?: mutableSetOf()
        roles.add(active)
        // admin accounts do not use the student/teacher switch
        roles.remove("admin")
        return RoleInfo(roles = roles, active = active)
    }

    /** Switch to a role the account ALREADY has. */
    suspend fun switchTo(role: String) {
        val info = getInfo()
        require(role in info.roles) { "You do not have the $role role yet." }
        val data = hashMapOf<String, Any>("role" to role, "roles" to info.roles.toList())
        db.collection(USERS).document(uid()).set(data, SetOptions.merge()).await()
    }

    /**
     * Adds a NEW role (stream + subjects picked by the user) and makes it the active one.
     * [isFree] / [fee] are used only when the new role is "teacher".
     */
    suspend fun addRole(
        role: String,
        stream: StudyStream,
        subjects: List<Subject>,
        isFree: Boolean,
        fee: Int
    ) {
        val info = getInfo()
        val data = hashMapOf<String, Any>(
            "role" to role,
            "roles" to (info.roles + role).toList(),
            "setupDone" to true,
            "${role}Study" to hashMapOf(
                "streamId" to stream.id,
                "streamName" to stream.name,
                "subjectIds" to subjects.map { it.id },
                "subjectNames" to subjects.map { it.name }
            )
        )
        if (role == "teacher") {
            data["isFree"] = isFree
            data["fee"] = if (isFree) 0 else fee
        }
        db.collection(USERS).document(uid()).set(data, SetOptions.merge()).await()
    }

    private companion object {
        const val USERS = "users"
    }
}