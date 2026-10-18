package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.AppNotification

/** Reads a user's notifications from the Firestore "notifications" collection. */
class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** The user's notifications, newest first. */
    suspend fun getNotifications(userId: String): List<AppNotification> =
        db.collection(COLLECTION).whereEqualTo("userId", userId).get().await().documents
            .map { it.toNotification() }
            .sortedByDescending { it.createdAtMillis }

    suspend fun markRead(ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = db.batch()
        ids.take(400).forEach { id -> batch.update(db.collection(COLLECTION).document(id), "read", true) }
        batch.commit().await()
    }

    private fun DocumentSnapshot.toNotification() = AppNotification(
        id = id,
        userId = getString("userId") ?: "",
        type = getString("type") ?: "",
        title = getString("title") ?: "",
        message = getString("message") ?: "",
        amount = getDouble("amount") ?: 0.0,
        receiptUrl = getString("receiptUrl") ?: "",
        read = getBoolean("read") ?: false,
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            ?: getLong("createdAtMillis")
            ?: 0L
    )

    private companion object {
        const val COLLECTION = "notifications"
    }
}