package lk.nenasayura.app.data.repository



import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AttemptAnswer
import lk.nenasayura.app.data.model.QuizAttempt

/**
 * Saves and loads a student's quiz attempts:
 *   users/{uid}/quiz_attempts/{id}
 *
 * Note: like the other repositories, a write waits for the server, so callers
 * should not block the screen on it (Firestore still sends it later when offline).
 */
class QuizAttemptRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun attemptsRef(uid: String) =
        db.collection(USERS).document(uid).collection(ATTEMPTS)

    suspend fun saveAttempt(uid: String, attempt: QuizAttempt) {
        val data = hashMapOf<String, Any>(
            "quizId" to attempt.quizId,
            "quizTitle" to attempt.quizTitle,
            "subjectId" to attempt.subjectId,
            "score" to attempt.score,
            "total" to attempt.total,
            "percent" to attempt.percent,
            "answers" to attempt.answers.map {
                mapOf(
                    "questionId" to it.questionId,
                    "selectedIndex" to it.selectedIndex,
                    "correct" to it.correct
                )
            },
            "createdAtMillis" to attempt.createdAtMillis,
            "createdAt" to FieldValue.serverTimestamp()
        )
        attemptsRef(uid).add(data).await()
    }

    /** The newest attempts first. A single-field order, so no Firestore index is needed. */
    suspend fun getAttempts(uid: String, limit: Long = 300): List<QuizAttempt> {
        val snapshot = attemptsRef(uid)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toAttempt() }
    }

    private fun DocumentSnapshot.toAttempt(): QuizAttempt? {
        val quizId = getString("quizId") ?: return null
        val total = (getLong("total") ?: 0L).toInt()
        if (total <= 0) return null

        val answers = (get("answers") as? List<*>)?.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            AttemptAnswer(
                questionId = map["questionId"] as? String ?: "",
                selectedIndex = (map["selectedIndex"] as? Number)?.toInt() ?: -1,
                correct = map["correct"] as? Boolean ?: false
            )
        } ?: emptyList()

        return QuizAttempt(
            id = id,
            quizId = quizId,
            quizTitle = getString("quizTitle") ?: "",
            subjectId = getString("subjectId") ?: "",
            score = (getLong("score") ?: 0L).toInt(),
            total = total,
            answers = answers,
            createdAtMillis = getLong("createdAtMillis") ?: 0L
        )
    }

    private companion object {
        const val USERS = "users"
        const val ATTEMPTS = "quiz_attempts"
    }
}