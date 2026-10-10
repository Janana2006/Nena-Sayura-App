package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AdminQuiz
import lk.nenasayura.app.data.model.NewQuestion

/**
 * Admin side of the quizzes. Same Firestore layout the student app already reads:
 *   quizzes/{quizId}                      title, subjectId, description, questionCount
 *   quizzes/{quizId}/questions/{id}       text, options, correctIndex, explanation, order
 *
 * Writes are sent in small batches (see CHUNK) so the Firestore rules, which check
 * "is this user an admin" once per write, stay inside Firestore's limit per batch.
 */
class QuizAdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getQuizzes(): List<AdminQuiz> =
        db.collection(QUIZZES).get().await().documents
            .mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                AdminQuiz(
                    id = doc.id,
                    title = title,
                    subjectId = doc.getString("subjectId") ?: "",
                    description = doc.getString("description") ?: "",
                    questionCount = (doc.getLong("questionCount") ?: -1L).toInt()
                )
            }
            .sortedBy { it.title.lowercase() }

    /** Creates a quiz (one lesson of one subject) together with all its questions. */
    suspend fun createQuiz(
        title: String,
        subjectId: String,
        description: String,
        questions: List<NewQuestion>
    ) {
        val quizRef = db.collection(QUIZZES).document()
        try {
            // First batch: the quiz document and the first few questions
            val first = questions.take(CHUNK - 1)
            val firstBatch = db.batch()
            firstBatch.set(
                quizRef,
                mapOf(
                    "title" to title.trim(),
                    "subjectId" to subjectId,
                    "description" to description.trim(),
                    "questionCount" to questions.size,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            first.forEachIndexed { index, q ->
                firstBatch.set(quizRef.collection(QUESTIONS).document(), questionData(q, index))
            }
            firstBatch.commit().await()

            // The rest of the questions
            questions.drop(first.size).chunked(CHUNK).forEachIndexed { chunkIndex, chunk ->
                val batch = db.batch()
                chunk.forEachIndexed { i, q ->
                    val order = first.size + chunkIndex * CHUNK + i
                    batch.set(quizRef.collection(QUESTIONS).document(), questionData(q, order))
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            // Do not leave a half-saved quiz behind
            try { deleteQuiz(quizRef.id) } catch (ignored: Exception) { }
            throw e
        }
    }

    /** Adds more questions to a quiz that already exists. */
    suspend fun addQuestions(quizId: String, questions: List<NewQuestion>) {
        val quizRef = db.collection(QUIZZES).document(quizId)
        val existing = quizRef.collection(QUESTIONS).get().await().size()

        questions.chunked(CHUNK).forEachIndexed { chunkIndex, chunk ->
            val batch = db.batch()
            chunk.forEachIndexed { i, q ->
                val order = existing + chunkIndex * CHUNK + i
                batch.set(quizRef.collection(QUESTIONS).document(), questionData(q, order))
            }
            batch.commit().await()
        }
        quizRef.update("questionCount", existing + questions.size).await()
    }

    /** Deletes the quiz and all its questions (Firestore does not delete sub-collections by itself). */
    suspend fun deleteQuiz(quizId: String) {
        val quizRef = db.collection(QUIZZES).document(quizId)
        val questionDocs = quizRef.collection(QUESTIONS).get().await().documents
        questionDocs.chunked(CHUNK).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        quizRef.delete().await()
    }

    private fun questionData(q: NewQuestion, order: Int): Map<String, Any> = mapOf(
        "text" to q.text.trim(),
        "options" to q.options.map { it.trim() },
        "correctIndex" to q.correctIndex,
        "explanation" to q.explanation.trim(),
        "order" to order
    )

    private companion object {
        const val QUIZZES = "quizzes"
        const val QUESTIONS = "questions"
        const val CHUNK = 15
    }
}