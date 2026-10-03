package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Payment

/** Reads payments and the admin commission setting from Firestore. */
class PaymentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** All payments made to one teacher, newest first. */
    suspend fun getTeacherPayments(teacherId: String): List<Payment> {
        val snapshot = db.collection(PAYMENTS)
            .whereEqualTo("teacherId", teacherId)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toPayment() }
            .sortedByDescending { it.createdAtMillis }
    }

    /** Current admin commission %, from app_settings/commission { percent: 10 }. */
    suspend fun getDefaultCommissionPercent(): Double {
        val doc = db.collection(SETTINGS).document(COMMISSION_DOC).get().await()
        return (doc.get("percent") as? Number)?.toDouble() ?: DEFAULT_PERCENT
    }

    // If the real field names differ, change them here only.
    private fun DocumentSnapshot.toPayment(): Payment? {
        val studentId = getString("studentId") ?: return null
        return Payment(
            id = id,
            studentId = studentId,
            studentName = getString("studentName") ?: "Student",
            teacherId = getString("teacherId") ?: "",
            subjectName = getString("subjectName") ?: "",
            amount = readAmount(get("amount")),
            status = getString("status") ?: "",
            commissionPercent = (get("commissionPercent") as? Number)?.toDouble(),
            createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    }

    // The amount may be saved as a number or as text; accept both.
    private fun readAmount(value: Any?): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private companion object {
        const val PAYMENTS = "payments"
        const val SETTINGS = "app_settings"
        const val COMMISSION_DOC = "commission"
        const val DEFAULT_PERCENT = 10.0
    }
}