package lk.nenasayura.app.data.repository



import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Paper

/** Reads papers from the Firestore "papers" collection. */
class PaperRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getPapers(): List<Paper> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents
            .mapNotNull { it.toPaper() }
            .sortedWith(compareByDescending<Paper> { it.year }.thenBy { it.title })
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toPaper(): Paper? {
        val title = getString("title") ?: return null
        val fileUrl = getString("fileUrl") ?: return null
        return Paper(
            id = id,
            title = title,
            subjectId = getString("subjectId") ?: "",
            year = readYear(get("year")),
            type = getString("type") ?: "",
            fileUrl = fileUrl
        )
    }

    // The year may be saved as a number or as text; accept both.
    private fun readYear(value: Any?): Int = when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }

    private companion object {
        const val COLLECTION = "papers"
    }
}