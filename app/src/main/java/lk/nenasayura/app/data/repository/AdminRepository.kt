package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AdminPermission
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.model.StudyStream

/**
 * Everything the Admin dashboard reads and writes.
 * Firestore rules decide who may do this: only users whose role is "admin".
 */
class AdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val subjects: SubjectRepository = SubjectRepository()
) {

    // ---------------- access ----------------

    suspend fun isAdmin(uid: String): Boolean {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role") == "admin"
    }

    /**
     * What this admin may open. null = not an admin at all.
     * Old admin accounts have no "permissions" field, so they get full access.
     */
    suspend fun getAdminAccess(uid: String): Set<AdminPermission>? {
        val doc = db.collection("users").document(uid).get().await()
        if (doc.getString("role") != "admin") return null
        val keys = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>()
        return AdminPermission.fromKeys(keys)
    }

    // ---------------- users ----------------

    suspend fun getUsers(): List<AppUser> =
        db.collection("users").get().await().documents
            .map { doc ->
                AppUser(
                    uid = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    role = doc.getString("role") ?: "student",
                    isFree = doc.getBoolean("isFree") ?: true,
                    fee = (doc.getLong("fee") ?: 0L).toInt(),
                    streamName = doc.getString("streamName") ?: "",
                    subjectNames = (doc.get("subjectNames") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList(),
                    staffRole = doc.getString("staffRole") ?: "",
                    permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>()
                )
            }
            .sortedBy { it.name.lowercase() }

    /**
     * Creates a new admin (staff) account WITHOUT logging the current admin out.
     * A normal createUser call would sign in as the new user, so a second
     * FirebaseApp instance is used just for creating the account.
     */
    suspend fun createStaff(
        name: String,
        email: String,
        password: String,
        staffRole: String,
        permissions: Set<AdminPermission>
    ) {
        val secondAuth = secondaryAuth()
        val created = secondAuth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = created.user ?: throw IllegalStateException("Could not create the account.")
        try {
            // Written with the ADMIN's login (primary Firestore), so the rules allow it.
            db.collection("users").document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "role" to "admin",
                    "staffRole" to staffRole,
                    "permissions" to permissions.map { it.key },
                    "setupDone" to true,
                    "isFree" to true,
                    "fee" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            // Do not leave a half-made account behind
            try { user.delete().await() } catch (ignored: Exception) { }
            throw e
        } finally {
            secondAuth.signOut()
        }
    }

    /** Changes the role and access of an existing staff member. */
    suspend fun updateStaffAccess(uid: String, staffRole: String, permissions: Set<AdminPermission>) {
        db.collection("users").document(uid).update(
            mapOf(
                "staffRole" to staffRole,
                "permissions" to permissions.map { it.key }
            )
        ).await()
    }

    private fun secondaryAuth(): FirebaseAuth {
        val primary = FirebaseApp.getInstance()
        val app = try {
            FirebaseApp.getInstance(SECOND_APP)
        } catch (e: IllegalStateException) {
            FirebaseApp.initializeApp(primary.applicationContext, primary.options, SECOND_APP)
        }
        return FirebaseAuth.getInstance(app)
    }

    // ---------------- payments ----------------

    suspend fun getPayments(): List<Payment> =
        db.collection("payments").get().await().documents
            .map { it.toPayment() }
            .sortedByDescending { it.createdAtMillis }

    suspend fun getCommissionPercent(): Double =
        db.collection("settings").document("commission").get().await()
            .getDouble("percent") ?: DEFAULT_COMMISSION

    suspend fun setCommissionPercent(percent: Double) {
        db.collection("settings").document("commission")
            .set(mapOf("percent" to percent)).await()
    }

    private fun DocumentSnapshot.toPayment() = Payment(
        id = id,
        studentId = getString("studentId") ?: "",
        studentName = getString("studentName") ?: "",
        teacherId = getString("teacherId") ?: "",
        teacherName = getString("teacherName") ?: "",
        subjectId = getString("subjectId") ?: "",
        subjectName = getString("subjectName") ?: "",
        amount = getDouble("amount") ?: 0.0,
        status = getString("status") ?: "paid",
        commissionPercent = getDouble("commissionPercent"),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            ?: getLong("createdAtMillis")
            ?: 0L
    )

    // ---------------- papers ----------------

    suspend fun getPapers(): List<Paper> =
        db.collection("papers").get().await().documents
            .mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                Paper(
                    id = doc.id,
                    title = title,
                    subjectId = doc.getString("subjectId") ?: "",
                    year = when (val y = doc.get("year")) {
                        is Number -> y.toInt()
                        is String -> y.toIntOrNull() ?: 0
                        else -> 0
                    },
                    type = doc.getString("type") ?: "",
                    fileUrl = doc.getString("fileUrl") ?: ""
                )
            }
            .sortedWith(compareByDescending<Paper> { it.year }.thenBy { it.title })

    /** [type] = "past", "model" or "provincial". */
    suspend fun addPaper(title: String, subjectId: String, year: Int, type: String, fileUrl: String) {
        db.collection("papers").add(
            mapOf(
                "title" to title.trim(),
                "subjectId" to subjectId,
                "year" to year,
                "type" to type,
                "fileUrl" to fileUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deletePaper(id: String) {
        db.collection("papers").document(id).delete().await()
    }

    // ---------------- guide books ----------------

    suspend fun getGuideBooks(): List<GuideBook> =
        db.collection("guideBooks").get().await().documents
            .mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                GuideBook(
                    id = doc.id,
                    title = title,
                    subjectId = doc.getString("subjectId") ?: "",
                    author = doc.getString("author") ?: "",
                    category = doc.getString("category") ?: "",
                    fileUrl = doc.getString("fileUrl") ?: ""
                )
            }
            .sortedBy { it.title }

    /** [category] = "guide" or "resource". */
    suspend fun addGuideBook(title: String, subjectId: String, author: String, category: String, fileUrl: String) {
        db.collection("guideBooks").add(
            mapOf(
                "title" to title.trim(),
                "subjectId" to subjectId,
                "author" to author.trim(),
                "category" to category,
                "fileUrl" to fileUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun deleteGuideBook(id: String) {
        db.collection("guideBooks").document(id).delete().await()
    }

    // ---------------- streams and subjects ----------------

    suspend fun getStreams(): List<StudyStream> = subjects.getStreams()

    suspend fun addStream(name: String) {
        val id = slug(name)
        require(id.isNotEmpty()) { "Enter a valid stream name." }
        val ref = db.collection("streams").document(id)
        check(!ref.get().await().exists()) { "A stream with this name already exists." }

        // "order" is needed: the app lists streams with orderBy("order")
        val all = db.collection("streams").get().await().documents
        val nextOrder = (all.maxOfOrNull { it.getLong("order") ?: 0L } ?: 0L) + 1
        ref.set(
            mapOf(
                "name" to name.trim(),
                "order" to nextOrder,
                "subjects" to emptyMap<String, String>()
            )
        ).await()
    }

    suspend fun deleteStream(streamId: String) {
        db.collection("streams").document(streamId).delete().await()
    }

    suspend fun addSubject(streamId: String, subjectName: String) {
        val id = slug(subjectName)
        require(id.isNotEmpty()) { "Enter a valid subject name." }
        val current = subjects.getSubjectsByStream(streamId)
        check(current.none { it.id == id }) { "This subject already exists in the stream." }
        val updated = current.associate { it.id to it.name } + (id to subjectName.trim())
        db.collection("streams").document(streamId).update("subjects", updated).await()
    }

    suspend fun deleteSubject(streamId: String, subjectId: String) {
        val updated = subjects.getSubjectsByStream(streamId)
            .filter { it.id != subjectId }
            .associate { it.id to it.name }
        db.collection("streams").document(streamId).update("subjects", updated).await()
    }

    // ---------------- files ----------------

    /** Uploads a file to Firebase Storage and returns its download link. */
    suspend fun uploadFile(folder: String, uri: Uri, fileName: String): String {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val ref = storage.reference.child("$folder/${System.currentTimeMillis()}_$safeName")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    // "Combined Maths" -> "combined_maths"
    private fun slug(text: String): String =
        text.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private companion object {
        const val SECOND_APP = "staff-creator"
        const val DEFAULT_COMMISSION = 10.0
    }
}