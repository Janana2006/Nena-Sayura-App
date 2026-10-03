package lk.nenasayura.app.data.repository



import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.QuizQuestion

/** Reads quizzes and their questions from Firestore. */
class QuizRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getQuizzes(): List<Quiz> {
        val snapshot = db.collection(QUIZZES).get().await()
        return snapshot.documents
            .mapNotNull { it.toQuiz() }
            .sortedBy { it.title }
    }

    suspend fun getQuiz(quizId: String): Quiz? =
        db.collection(QUIZZES).document(quizId).get().await().toQuiz()

    suspend fun getQuestions(quizId: String): List<QuizQuestion> {
        val snapshot = db.collection(QUIZZES).document(quizId)
            .collection(QUESTIONS).get().await()

        // Keep the admin's "order" if present, otherwise sort by document id
        return snapshot.documents
            .mapNotNull { doc ->
                val question = doc.toQuestion() ?: return@mapNotNull null
                val order = (doc.get("order") as? Number)?.toLong() ?: Long.MAX_VALUE
                order to question
            }
            .sortedWith(compareBy<Pair<Long, QuizQuestion>>({ it.first }, { it.second.id }))
            .map { it.second }
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toQuiz(): Quiz? {
        val title = getString("title") ?: return null
        return Quiz(
            id = id,
            title = title,
            subjectId = getString("subjectId") ?: "",
            description = getString("description") ?: ""
        )
    }

    private fun DocumentSnapshot.toQuestion(): QuizQuestion? {
        val text = getString("text") ?: return null
        val options = (get("options") as? List<*>)?.mapNotNull { it?.toString() } ?: return null
        val correctIndex = (get("correctIndex") as? Number)?.toInt() ?: return null
        if (options.size < 2 || correctIndex !in options.indices) return null
        return QuizQuestion(
            id = id,
            text = text,
            options = options,
            correctIndex = correctIndex,
            explanation = getString("explanation") ?: ""
        )
    }

    private companion object {
        const val QUIZZES = "quizzes"
        const val QUESTIONS = "questions"
    }
}