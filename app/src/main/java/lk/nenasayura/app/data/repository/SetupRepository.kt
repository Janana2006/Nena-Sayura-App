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

    fun getStreams(onResult: (Result<List<StudyStream>>) -> Unit) {
        db.collection("streams").orderBy("order").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val subjects = (doc.get("subjects") as? List<*>)?.mapNotNull { item ->
                        val map = item as? Map<*, *> ?: return@mapNotNull null
                        val id = map["id"] as? String ?: return@mapNotNull null
                        val name = map["name"] as? String ?: return@mapNotNull null
                        Subject(id = id, name = name)
                    } ?: emptyList()

                    StudyStream(
                        id = doc.id,
                        name = doc.getString("name") ?: doc.id,
                        subjects = subjects
                    )
                }
                onResult(Result.success(list))
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun saveTeacherSetup(
        stream: StudyStream,
        subjects: List<Subject>,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onResult(Result.failure(Exception("Not logged in")))
            return
        }

        val data = mapOf(
            "streamId" to stream.id,
            "streamName" to stream.name,
            "subjectIds" to subjects.map { it.id },
            "subjectNames" to subjects.map { it.name },
            "setupDone" to true
        )

        db.collection("users").document(uid).set(data, SetOptions.merge())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onResult(Result.success(Unit))
                else onResult(Result.failure(task.exception ?: Exception("Failed to save")))
            }
    }
}