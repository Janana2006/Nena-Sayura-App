package lk.nenasayura.app.data.repository



import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Subject

/** Reads A/L subjects from the Firestore "subjects" collection. */
class SubjectRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getSubjects(): List<Subject> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents
            .mapNotNull { it.toSubject() }
            .sortedBy { it.name }
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toSubject(): Subject? {
        val name = getString("name") ?: return null
        return Subject(
            id = id,
            name = name,
            stream = getString("stream") ?: ""
        )
    }

    private companion object {
        const val COLLECTION = "subjects"
    }
}