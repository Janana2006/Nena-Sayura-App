package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.MarkingScheme

/**
 * Admin side of the marking schemes: the same "markingSchemes" collection the
 * student app reads (title, subjectId, year, type, fileUrl).
 */
class MarkingSchemeAdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val files: AdminRepository = AdminRepository()
) {

    suspend fun getMarkingSchemes(): List<MarkingScheme> =
        db.collection(COLLECTION).get().await().documents
            .mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                MarkingScheme(
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
            .sortedWith(compareByDescending<MarkingScheme> { it.year }.thenBy { it.title })

    /** Uploads the PDF to Cloudinary (same uploader the papers use) and returns its link. */
    suspend fun uploadFile(type: String, uri: Uri, fileName: String): String =
        files.uploadFile("marking_schemes/$type", uri, fileName)

    /** [type] = "past", "model" or "provincial". */
    suspend fun addMarkingScheme(title: String, subjectId: String, year: Int, type: String, fileUrl: String) {
        db.collection(COLLECTION).add(
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

    suspend fun deleteMarkingScheme(id: String) {
        db.collection(COLLECTION).document(id).delete().await()
    }

    private companion object {
        const val COLLECTION = "markingSchemes"
    }
}