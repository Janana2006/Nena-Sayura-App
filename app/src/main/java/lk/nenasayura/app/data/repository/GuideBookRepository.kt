package lk.nenasayura.app.data.repository



import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.GuideBook

/** Reads guide books from the Firestore "guideBooks" collection. */
class GuideBookRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getGuideBooks(): List<GuideBook> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents
            .mapNotNull { it.toGuideBook() }
            .sortedBy { it.title }
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toGuideBook(): GuideBook? {
        val title = getString("title") ?: return null
        val fileUrl = getString("fileUrl") ?: return null
        return GuideBook(
            id = id,
            title = title,
            subjectId = getString("subjectId") ?: "",
            author = getString("author") ?: "",
            fileUrl = fileUrl
        )
    }

    private companion object {
        const val COLLECTION = "guideBooks"
    }
}