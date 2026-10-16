package lk.nenasayura.app.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.model.TeacherProfile

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

    /** Ids of the teachers this student has paid (used to unlock PAID material). */
    suspend fun getPaidTeacherIds(studentId: String): Set<String> {
        return try {
            val snapshot = db.collection(COLLECTION)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            snapshot.documents
                .map { it.toPayment() }
                .filter { it.isPaid }
                .map { it.teacherId }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * A student pays a teacher's fee. The record goes to the "payments" collection, which the
     * teacher's earnings and the admin's payments tab already read.
     * If the student has already paid this teacher nothing new is saved (no double charge).
     */
    suspend fun payTeacher(studentId: String, teacher: TeacherProfile, method: String) {
        require(!teacher.isFree && teacher.fee > 0) { "This teacher is free. No payment is needed." }

        if (teacher.uid in getPaidTeacherIds(studentId)) return

        val studentName = try {
            db.collection("users").document(studentId).get().await().getString("name") ?: ""
        } catch (e: Exception) {
            ""
        }

        val data = hashMapOf<String, Any>(
            "studentId" to studentId,
            "studentName" to studentName,
            "teacherId" to teacher.uid,
            "teacherName" to teacher.name,
            "subjectId" to (teacher.subjectIds.firstOrNull() ?: ""),
            "subjectName" to teacher.subjectNames.joinToString(", "),
            "amount" to teacher.fee.toDouble(),
            "status" to "paid",
            "method" to method,
            "commissionPercent" to getDefaultCommissionPercent(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection(COLLECTION).add(data).await()
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