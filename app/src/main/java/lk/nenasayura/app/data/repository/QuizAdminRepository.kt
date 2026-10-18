package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AdminQuiz
import lk.nenasayura.app.data.model.NewQuestion
import lk.nenasayura.app.data.model.QuizQuestion

class QuizAdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getQuizzes(): List<AdminQuiz> {
        val snapshot = db.collection("quizzes").get().await()
        return snapshot.documents.map { doc ->
            val count = (doc.get("questionCount") as? Number)?.toInt() ?: -1
            AdminQuiz(
                id = doc.id,
                title = doc.getString("title") ?: "",
                subjectId = doc.getString("subjectId") ?: "",
                description = doc.getString("description") ?: "",
                questionCount = count
            )
        }.sortedBy { it.title }
    }

    /** The questions of one quiz, in the order the admin added them. */
    suspend fun getQuestions(quizId: String): List<QuizQuestion> {
        val snapshot = db.collection("quizzes").document(quizId)
            .collection("questions").get().await()
        return snapshot.documents
            .mapNotNull { doc ->
                val text = doc.getString("text") ?: return@mapNotNull null
                val options = (doc.get("options") as? List<*>)?.mapNotNull { it?.toString() }
                    ?: return@mapNotNull null
                val order = (doc.get("order") as? Number)?.toLong() ?: Long.MAX_VALUE
                order to QuizQuestion(
                    id = doc.id,
                    text = text,
                    options = options,
                    correctIndex = (doc.get("correctIndex") as? Number)?.toInt() ?: -1,
                    explanation = doc.getString("explanation") ?: ""
                )
            }
            .sortedWith(compareBy<Pair<Long, QuizQuestion>>({ it.first }, { it.second.id }))
            .map { it.second }
    }

    suspend fun createQuiz(
        subjectId: String,
        title: String,
        description: String,
        questions: List<NewQuestion>
    ): String {
        val docRef = db.collection("quizzes").add(
            mapOf(
                "subjectId" to subjectId,
                "title" to title.trim(),
                "description" to description.trim(),
                "questionCount" to questions.size,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val batch = db.batch()
        questions.forEachIndexed { index, q ->
            val qRef = docRef.collection("questions").document()
            batch.set(
                qRef,
                mapOf(
                    "text" to q.text.trim(),
                    "options" to q.options.map { it.trim() },
                    "correctIndex" to q.correctIndex,
                    "explanation" to q.explanation.trim(),
                    "order" to index + 1
                )
            )
        }
        batch.commit().await()
        return docRef.id
    }

    suspend fun addQuestions(quizId: String, newQuestions: List<NewQuestion>) {
        val quizRef = db.collection("quizzes").document(quizId)
        val currentQuestions = quizRef.collection("questions").get().await()
        val startOrder = currentQuestions.size() + 1

        val batch = db.batch()
        newQuestions.forEachIndexed { index, q ->
            val qRef = quizRef.collection("questions").document()
            batch.set(
                qRef,
                mapOf(
                    "text" to q.text.trim(),
                    "options" to q.options.map { it.trim() },
                    "correctIndex" to q.correctIndex,
                    "explanation" to q.explanation.trim(),
                    "order" to startOrder + index
                )
            )
        }
        batch.update(quizRef, "questionCount", FieldValue.increment(newQuestions.size.toLong()))
        batch.commit().await()
    }

    suspend fun deleteQuiz(quizId: String) {
        val quizRef = db.collection("quizzes").document(quizId)
        val questions = quizRef.collection("questions").get().await()
        val batch = db.batch()
        questions.documents.forEach { batch.delete(it.reference) }
        batch.delete(quizRef)
        batch.commit().await()
    }
}