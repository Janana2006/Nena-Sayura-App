package lk.nenasayura.app.data.repository



import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.MarkingScheme

/** Reads marking schemes from the Firestore "markingSchemes" collection. */
class MarkingSchemeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getMarkingSchemes(): List<MarkingScheme> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents
            .mapNotNull { it.toMarkingScheme() }
            .sortedWith(compareByDescending<MarkingScheme> { it.year }.thenBy { it.title })
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toMarkingScheme(): MarkingScheme? {
        val title = getString("title") ?: return null
        val fileUrl = getString("fileUrl") ?: return null
        return MarkingScheme(
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
        const val COLLECTION = "markingSchemes"
    }
}