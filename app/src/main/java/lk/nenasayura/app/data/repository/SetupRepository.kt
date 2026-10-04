package lk.nenasayura.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject

class SetupRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // streams collection eken okkoma streams + subjects
    fun getStreams(onResult: (Result<List<StudyStream>>) -> Unit) {
        db.collection("streams").orderBy("order").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    StudyStream(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id,
                        subjects = parseSubjects(doc.get("subjects"))
                    )
                }
                onResult(Result.success(list))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    // subjects format 2kama handle karanawa
    private fun parseSubjects(raw: Any?): List<Subject> {
        return when (raw) {

            // Format 1: subjects = map  { physics: "Physics", chemistry: "Chemistry" }
            is Map<*, *> -> raw.entries
                .mapNotNull { entry ->
                    val id = entry.key as? String ?: return@mapNotNull null
                    val name = entry.value as? String ?: return@mapNotNull null
                    Subject(id = id, name = name)
                }
                .sortedBy { it.name }

            // Format 2: subjects = array of maps
            is List<*> -> raw.flatMap { item ->
                val map = item as? Map<*, *> ?: return@flatMap emptyList<Subject>()
                val id = map["id"] as? String
                val name = map["name"] as? String

                if (id != null && name != null) {
                    // { id: "physics", name: "Physics" }
                    listOf(Subject(id = id, name = name))
                } else {
                    // { physics: "Physics" }
                    map.entries.mapNotNull { entry ->
                        val key = entry.key as? String ?: return@mapNotNull null
                        val value = entry.value as? String ?: return@mapNotNull null
                        Subject(id = key, name = value)
                    }
                }
            }

            else -> emptyList()
        }
    }

    // CHANGED: teacher saha student dekatama (role anuwa) stream/subjects save karala setupDone = true
    fun saveSetup(
        role: String,
        stream: StudyStream,
        subjects: List<Subject>,
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(Exception("Not logged in")))
            return
        }

        val data = hashMapOf<String, Any>(
            "uid" to user.uid,
            "role" to role,
            "streamId" to stream.id,
            "streamName" to stream.name,
            "subjectIds" to subjects.map { it.id },
            "subjectNames" to subjects.map { it.name },
            "setupDone" to true
        )
        // Google login wage profile nathi ayata name/email dala thiyaganna
        user.email?.let { data["email"] = it }
        user.displayName?.takeIf { it.isNotBlank() }?.let { data["name"] = it }

        db.collection("users").document(user.uid).set(data, SetOptions.merge())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Failed to save")))
            }
    }
}