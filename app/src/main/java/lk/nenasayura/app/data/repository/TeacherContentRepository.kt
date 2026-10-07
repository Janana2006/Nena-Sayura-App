package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherContent

/**
 * Teacher uploads: the file goes to Firebase Storage, the details go to the
 * Firestore "teacher_content" collection.
 */
class TeacherContentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    /** Everything one teacher has uploaded, newest first. */
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
        videoUrl: String
    ) {
        var fileUrl = ""
        var storagePath = ""

        // 1) Files (not video links) go to Firebase Storage first.
        if (type != ContentType.VIDEO_LINK && fileUri != null) {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            storagePath = "teacher_content/$teacherId/${System.currentTimeMillis()}_$safeName"
            val ref = storage.reference.child(storagePath)
            ref.putFile(fileUri).await()
            fileUrl = ref.downloadUrl.await().toString()
        }

        // 2) Save the details in Firestore.
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
            "storagePath" to storagePath,
            "videoUrl" to videoUrl.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection(COLLECTION).add(data).await()
    }

    /** Edits the text and the Free/Paid choice (the file itself is not replaced). */
    suspend fun update(id: String, title: String, description: String, access: AccessType) {
        db.collection(COLLECTION).document(id).update(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "access" to access.key
            )
        ).await()
    }

    /** Deletes the Firestore record and (if there is one) the file in Storage. */
    suspend fun delete(content: TeacherContent) {
        db.collection(COLLECTION).document(content.id).delete().await()
        if (content.storagePath.isNotBlank()) {
            try {
                storage.reference.child(content.storagePath).delete().await()
            } catch (e: Exception) {
                // The record is already gone; a leftover file is harmless, so don't fail.
            }
        }
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
        videoUrl = getString("videoUrl") ?: "",
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
    )

    private companion object {
        const val COLLECTION = "teacher_content"
    }
}