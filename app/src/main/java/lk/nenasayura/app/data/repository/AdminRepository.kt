package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AdminPermission
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.util.CloudinaryUploader

/**
 * Everything the Admin dashboard reads and writes.
 * Firestore rules decide who may do this: only users whose role is "admin".
 */
class AdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
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
                    fee = (doc.get("fee") as? Number)?.toInt() ?: 0,
                    streamName = doc.getString("streamName") ?: "",
                    subjectNames = (doc.get("subjectNames") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList(),
                    staffRole = doc.getString("staffRole") ?: "",
                    permissions = (doc.get("permissions") as? List<*>)?.filterIsInstance<String>(),
                    loginMethod = doc.getString("loginMethod") ?: ""
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
                    "loginMethod" to "email",
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
                    fileUrl = doc.getString("fileUrl") ?: "",
                    term = (doc.get("term") as? Number)?.toInt() ?: 0,
                    province = doc.getString("province") ?: ""
                )
            }
            .sortedWith(compareByDescending<Paper> { it.year }.thenBy { it.title })

    /** [type] = "past", "model" or "provincial". [term] (1-3) and [province] are only saved for provincial papers. */
    suspend fun addPaper(title: String, subjectId: String, year: Int, type: String, fileUrl: String, term: Int = 0, province: String = "") {
        val data = mutableMapOf<String, Any>(
            "title" to title.trim(),
            "subjectId" to subjectId,
            "year" to year,
            "type" to type,
            "fileUrl" to fileUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (type == "provincial" && term in 1..3) data["term"] = term
        if (type == "provincial" && province.isNotBlank()) data["province"] = province
        db.collection("papers").add(data).await()
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

    // ---------------- streams, subjects and kandas ----------------

    /**
     * Streams with their subjects. [StudyStream.groups] holds the kandas: group 0 is
     * Kanda 1, group 1 is Kanda 2 ... Empty kandas are kept so the position stays stable.
     */
    suspend fun getStreams(): List<StudyStream> {
        val streams = subjects.getStreams()
        val rawGroups = db.collection("streams").get().await().documents
            .associate { it.id to it.get("groups") }

        return streams.map { stream ->
            val byId = stream.subjects.associateBy { it.id }
            val kandas = readKandas(rawGroups[stream.id]).map { ids -> ids.mapNotNull { byId[it] } }
            val padding = List(maxOf(0, KANDA_MIN - kandas.size)) { emptyList<lk.nenasayura.app.data.model.Subject>() }
            stream.copy(groups = kandas + padding)
        }
    }

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

    /** Adds a subject to the stream and puts it in Kanda [kanda] (1, 2, 3 ...). */
    suspend fun addSubject(streamId: String, subjectName: String, kanda: Int) {
        val id = slug(subjectName)
        require(id.isNotEmpty()) { "Enter a valid subject name." }
        val current = subjects.getSubjectsByStream(streamId)
        check(current.none { it.id == id }) { "This subject already exists in the stream." }

        val updated = current.associate { it.id to it.name } + (id to subjectName.trim())
        val kandas = placeInKanda(loadKandas(streamId), id, kanda)
        saveStream(streamId, updated, kandas)
    }

    /** Moves a subject that is already in the stream into Kanda [kanda]. */
    suspend fun moveSubject(streamId: String, subjectId: String, kanda: Int) {
        val current = subjects.getSubjectsByStream(streamId).associate { it.id to it.name }
        check(current.containsKey(subjectId)) { "This subject is no longer in the stream." }
        saveStream(streamId, current, placeInKanda(loadKandas(streamId), subjectId, kanda))
    }

    suspend fun deleteSubject(streamId: String, subjectId: String) {
        val updated = subjects.getSubjectsByStream(streamId)
            .filter { it.id != subjectId }
            .associate { it.id to it.name }
        val kandas = loadKandas(streamId).map { ids -> ids.filter { it != subjectId } }
        saveStream(streamId, updated, kandas)
    }

    private suspend fun loadKandas(streamId: String): List<List<String>> {
        val doc = db.collection("streams").document(streamId).get().await()
        return readKandas(doc.get("groups"))
    }

    // The student setup reads "groups" as text, one text per kanda, "|" between the options:
    //   ["combined_maths", "physics", "chemistry|ict"]
    //   one subject in a kanda = compulsory, two or more = the student chooses one
    private fun readKandas(raw: Any?): List<List<String>> =
        (raw as? List<*>)?.map { item ->
            (item as? String).orEmpty().split("|").map { it.trim() }.filter { it.isNotEmpty() }
        } ?: emptyList()

    // Takes the subject out of every kanda, then puts it into the chosen one
    private fun placeInKanda(kandas: List<List<String>>, subjectId: String, kanda: Int): List<List<String>> {
        val size = maxOf(kandas.size, kanda)
        return List(size) { index ->
            val ids = kandas.getOrNull(index).orEmpty().filter { it != subjectId }
            if (index == kanda - 1) ids + subjectId else ids
        }
    }

    private suspend fun saveStream(streamId: String, subjectMap: Map<String, String>, kandas: List<List<String>>) {
        val groups = kandas.map { it.joinToString("|") }.dropLastWhile { it.isEmpty() }
        db.collection("streams").document(streamId)
            .update("subjects", subjectMap, "groups", groups).await()
    }

    // ---------------- files ----------------

    /** Uploads a file (PDF or image) to Cloudinary and returns its https link. */
    suspend fun uploadFile(folder: String, uri: Uri, fileName: String): String {
        val context = FirebaseApp.getInstance().applicationContext
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return CloudinaryUploader
            .uploadFile(context, uri, "${System.currentTimeMillis()}_$safeName", "nena_sayura/$folder")
            .getOrThrow()
    }

    // "Combined Maths" -> "combined_maths"
    private fun slug(text: String): String =
        text.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private companion object {
        const val SECOND_APP = "staff-creator"
        const val DEFAULT_COMMISSION = 10.0
        const val KANDA_MIN = 3
    }
}