package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.Subject

/** Saves teacher uploads: the file goes to Storage, the details go to Firestore. */
class TeacherContentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

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

    private companion object {
        const val COLLECTION = "teacher_content"
    }
}