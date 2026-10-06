package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Payment

/**
 * Repository for handling payment records from Firestore collection "payments".
 */
class PaymentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Returns the default admin commission percentage (e.g., 10.0%). */
    suspend fun getDefaultCommissionPercent(): Double {
        return try {
            val doc = db.collection(SETTINGS_COLLECTION).document("commission").get().await()
            doc.getDouble("percent") ?: DEFAULT_COMMISSION
        } catch (e: Exception) {
            DEFAULT_COMMISSION
        }
    }

    /** Fetches all payments associated with a specific teacher. */
    suspend fun getTeacherPayments(teacherId: String): List<Payment> {
        return try {
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()

            snapshot.documents.map { it.toPayment() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun DocumentSnapshot.toPayment(): Payment {
        return Payment(
            id = id,
            studentId = getString("studentId") ?: "",
            studentName = getString("studentName") ?: "",
            teacherId = getString("teacherId") ?: "",
            teacherName = getString("teacherName") ?: "",
            subjectId = getString("subjectId") ?: "",
            subjectName = getString("subjectName") ?: "",
            amount = getDouble("amount") ?: 0.0,
            status = getString("status") ?: "paid",
            commissionPercent = getDouble("commissionPercent"),
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
                ?: getLong("createdAtMillis")
                ?: 0L
        )
    }

    companion object {
        private const val COLLECTION = "payments"
        private const val SETTINGS_COLLECTION = "settings"
        private const val DEFAULT_COMMISSION = 10.0
    }
}
