package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentStatus
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.util.CloudinaryUploader

/**
 * Teacher uploads: the file goes to Cloudinary, the details go to the
 * Firestore "teacher_content" collection.
 *
 * No approval step: when a teacher uploads, students can see it right away.
 * The admin sees every upload in the admin dashboard and can remove any of them.
 */
class TeacherContentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // ---------------- teacher ----------------

    /** Everything one teacher has uploaded (every status), newest first. */
    suspend fun getContents(teacherId: String): List<TeacherContent> {
        val snapshot = db.collection(COLLECTION)
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        // Sorted here (not in the query) so no Firestore composite index is needed.
        return snapshot.documents
            .map { it.toContent() }
            .sortedByDescending { it.createdAtMillis }
    }

    suspend fun upload(
        teacherId: String,
        teacherName: String,
        title: String,
        description: String,
        subject: Subject,
        type: ContentType,
        access: AccessType,
        fileUri: Uri?,
        fileName: String,
        videoUrl: String,
        answerFileUri: Uri? = null,
        answerFileName: String = ""
    ) {
        var fileUrl = ""
        var answerFileUrl = ""
        val context = FirebaseApp.getInstance().applicationContext

        // 1) Files (not video links) go to Cloudinary first.
        if (type != ContentType.VIDEO_LINK && fileUri != null) {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            fileUrl = CloudinaryUploader
                .uploadFile(
                    context,
                    fileUri,
                    "${System.currentTimeMillis()}_$safeName",
                    "nena_sayura/teacher_content/$teacherId"
                )
                .getOrThrow()
        }

        // 1.1) Upload Answer Sheet if provided (for Model Paper or others)
        if (answerFileUri != null) {
            val safeAnsName = answerFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            answerFileUrl = CloudinaryUploader
                .uploadFile(
                    context,
                    answerFileUri,
                    "${System.currentTimeMillis()}_ans_$safeAnsName",
                    "nena_sayura/teacher_content/$teacherId"
                )
                .getOrThrow()
        }

        // 2) Save the details in Firestore. The upload is live for students at once.
        val data = hashMapOf(
            "teacherId" to teacherId,
            "teacherName" to teacherName,
            "title" to title.trim(),
            "description" to description.trim(),
            "subjectId" to subject.id,
            "subjectName" to subject.name,
            "type" to type.key,
            "access" to access.key,
            "fileUrl" to fileUrl,
            "fileName" to fileName,
            "storagePath" to "",
            "answerFileUrl" to answerFileUrl,
            "answerFileName" to answerFileName,
            "answerStoragePath" to "",
            "videoUrl" to videoUrl.trim(),
            "status" to ContentStatus.APPROVED.key,
            "declineReason" to "",
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection(COLLECTION).add(data).await()
    }

    /**
     * Edits the text and the Free/Paid choice (the file itself is not replaced).
     * Students see the change right away.
     */
    suspend fun update(id: String, title: String, description: String, access: AccessType) {
        db.collection(COLLECTION).document(id).update(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "access" to access.key
            )
        ).await()
    }

    /**
     * Deletes the Firestore record. The file on Cloudinary is not removed
     * (that needs the API secret); delete it by hand in the Media Library if needed.
     */
    suspend fun delete(content: TeacherContent) {
        db.collection(COLLECTION).document(content.id).delete().await()
    }

    // ---------------- student ----------------

    /**
     * Everything students can see, newest first. There is no approval step, so every
     * upload counts. Only items an old review had DECLINED stay hidden.
     * (When the admin removes an upload it is deleted, so it is simply gone.)
     */
    suspend fun getVisibleContents(): List<TeacherContent> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents
            .map { it.toContent() }
            .filter { it.status != ContentStatus.DECLINED }
            .sortedByDescending { it.createdAtMillis }
    }

    /** The same, for ONE teacher (the student's teacher page). */
    suspend fun getVisibleContents(teacherId: String): List<TeacherContent> {
        val snapshot = db.collection(COLLECTION)
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        return snapshot.documents
            .map { it.toContent() }
            .filter { it.status != ContentStatus.DECLINED }
            .sortedByDescending { it.createdAtMillis }
    }

    // ---------------- admin ----------------

    /**
     * Every upload of every teacher, newest first, for the admin dashboard.
     * Old documents made before the approval flow have no "status"; they are
     * marked "approved" here once, so they keep showing to students.
     */
    suspend fun getAllForReview(): List<TeacherContent> {
        val snapshot = db.collection(COLLECTION).get().await()

        val old = snapshot.documents.filter { !it.contains("status") }
        if (old.isNotEmpty()) {
            try {
                old.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.update(it.reference, "status", ContentStatus.APPROVED.key) }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                // Not fatal: the list below still treats them as approved.
            }
        }

        return snapshot.documents
            .map { it.toContent() }
            .sortedByDescending { it.createdAtMillis }
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toContent() = TeacherContent(
        id = id,
        teacherId = getString("teacherId") ?: "",
        teacherName = getString("teacherName") ?: "",
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        subjectId = getString("subjectId") ?: "",
        subjectName = getString("subjectName") ?: "",
        type = ContentType.fromKey(getString("type")),
        access = AccessType.fromKey(getString("access")),
        fileUrl = getString("fileUrl") ?: "",
        fileName = getString("fileName") ?: "",
        storagePath = getString("storagePath") ?: "",
        answerFileUrl = getString("answerFileUrl") ?: "",
        answerFileName = getString("answerFileName") ?: "",
        answerStoragePath = getString("answerStoragePath") ?: "",
        videoUrl = getString("videoUrl") ?: "",
        status = ContentStatus.fromKey(getString("status")),
        declineReason = getString("declineReason") ?: "",
        reviewedAtMillis = getTimestamp("reviewedAt")?.toDate()?.time ?: 0L,
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
    )

    private companion object {
        const val COLLECTION = "teacher_content"
    }
}