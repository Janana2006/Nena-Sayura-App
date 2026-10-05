package lk.nenasayura.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Review

/** Reads and writes student reviews in the Firestore "reviews" collection. */
class ReviewRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** All reviews, newest first (reviews without a date go last). */
    suspend fun getReviews(): List<Review> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents
            .mapNotNull { it.toReview() }
            .sortedByDescending { it.createdAtMillis }
    }

    /** The review this student wrote earlier, or null. */
    suspend fun getReview(uid: String): Review? =
        db.collection(COLLECTION).document(uid).get().await().toReview()

    /** Creates the student's review, or replaces it if they already wrote one. */
    suspend fun saveReview(uid: String, studentName: String, rating: Int, comment: String) {
        val data = hashMapOf(
            "userId" to uid,
            "studentName" to studentName,
            "rating" to rating,
            "comment" to comment,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection(COLLECTION).document(uid).set(data).await()
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toReview(): Review? {
        val comment = getString("comment") ?: return null
        val rating = (get("rating") as? Number)?.toInt()?.coerceIn(1, 5) ?: return null
        val createdAt = (get("createdAt") as? Timestamp)?.toDate()?.time ?: 0L
        return Review(
            id = id,
            studentName = getString("studentName")?.ifBlank { null } ?: "Student",
            rating = rating,
            comment = comment,
            createdAtMillis = createdAt
        )
    }

    private companion object {
        const val COLLECTION = "reviews"
    }
}