package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Subject

/** Reads A/L subjects from the Firestore "streams" collection. */
class SubjectRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Okkoma subjects (Physics wage stream 2kakama thiyena ewa ekak witharai)
    suspend fun getSubjects(): List<Subject> {
        val snapshot = db.collection(COLLECTION).orderBy("order").get().await()
        return snapshot.documents
            .flatMap { doc -> parseSubjects(doc.get("subjects"), doc.id) }
            .distinctBy { it.id }
            .sortedBy { it.name }
    }

    // Stream ekakata adala subjects (e.g. "maths", "bio")
    suspend fun getSubjectsByStream(streamId: String): List<Subject> {
        val doc = db.collection(COLLECTION).document(streamId).get().await()
        return parseSubjects(doc.get("subjects"), doc.id).sortedBy { it.name }
    }

    // subjects format 2kama handle karanawa
    // If the real field names differ, change them here only.
    private fun parseSubjects(raw: Any?, streamId: String): List<Subject> {
        return when (raw) {

            // { physics: "Physics", chemistry: "Chemistry" }
            is Map<*, *> -> raw.entries.mapNotNull { entry ->
                val id = entry.key as? String ?: return@mapNotNull null
                val name = entry.value as? String ?: return@mapNotNull null
                Subject(id = id, name = name, stream = streamId)
            }

            // array of maps: [ { physics: "Physics" }, { id: "x", name: "X" } ]
            is List<*> -> raw.flatMap { item ->
                val map = item as? Map<*, *> ?: return@flatMap emptyList<Subject>()
                val id = map["id"] as? String
                val name = map["name"] as? String

                if (id != null && name != null) {
                    listOf(Subject(id = id, name = name, stream = streamId))
                } else {
                    map.entries.mapNotNull { entry ->
                        val key = entry.key as? String ?: return@mapNotNull null
                        val value = entry.value as? String ?: return@mapNotNull null
                        Subject(id = key, name = value, stream = streamId)
                    }
                }
            }

            else -> emptyList()
        }
    }

    private companion object {
        const val COLLECTION = "streams"
    }
}