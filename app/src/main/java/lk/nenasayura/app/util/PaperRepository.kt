package lk.nenasayura.app.util

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Paper(
    val id: String = "",
    val title: String = "",
    val streamId: String = "",
    val subjectId: String = "",
    val category: String = "",
    val url: String = "",
    val uploadedBy: String = ""
)

object PaperRepository {

    suspend fun savePaper(
        title: String,
        streamId: String,
        subjectId: String,
        category: String,
        url: String,
        uploadedBy: String
    ): Result<Unit> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val paperData = hashMapOf(
                "title" to title,
                "streamId" to streamId,
                "subjectId" to subjectId,
                "category" to category,
                "url" to url,
                "uploadedBy" to uploadedBy,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("papers").add(paperData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPapers(
        streamId: String,
        subjectId: String,
        category: String
    ): Result<List<Paper>> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("papers")
                .whereEqualTo("streamId", streamId)
                .whereEqualTo("subjectId", subjectId)
                .whereEqualTo("category", category)
                .get()
                .await()

            val papers = snapshot.documents.map { doc ->
                Paper(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    streamId = doc.getString("streamId").orEmpty(),
                    subjectId = doc.getString("subjectId").orEmpty(),
                    category = doc.getString("category").orEmpty(),
                    url = doc.getString("url").orEmpty(),
                    uploadedBy = doc.getString("uploadedBy").orEmpty()
                )
            }
            Result.success(papers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
