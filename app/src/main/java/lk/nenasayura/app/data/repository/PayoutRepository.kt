package lk.nenasayura.app.data.repository

import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Payout
import lk.nenasayura.app.util.CloudinaryUploader
import lk.nenasayura.app.util.rs

/**
 * Payments the admin makes to teachers (collection "payouts").
 * Every payout also creates a notification for the teacher, in the same batch write,
 * so a payout can never exist without the teacher being told.
 */
class PayoutRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Every payout of every teacher, newest first (Admin > Payments > History). */
    suspend fun getAllPayouts(): List<Payout> =
        db.collection(PAYOUTS).get().await().documents
            .map { it.toPayout() }
            .sortedByDescending { it.createdAtMillis }

    /** The payouts of one teacher, newest first (Teacher > Payments > history). */
    suspend fun getTeacherPayouts(teacherId: String): List<Payout> =
        db.collection(PAYOUTS).whereEqualTo("teacherId", teacherId).get().await().documents
            .map { it.toPayout() }
            .sortedByDescending { it.createdAtMillis }

    /**
     * Uploads the receipt, then saves the payout and the teacher's notification together.
     * If the upload fails nothing is saved.
     */
    suspend fun createPayout(
        teacherId: String,
        teacherName: String,
        amount: Double,
        reference: String,
        receiptUri: Uri,
        receiptName: String,
        adminUid: String,
        adminName: String
    ) {
        val receiptUrl = uploadReceipt(receiptUri, receiptName)
        val now = System.currentTimeMillis()

        val payoutRef = db.collection(PAYOUTS).document()
        val notificationRef = db.collection(NOTIFICATIONS).document()

        val batch = db.batch()
        batch.set(
            payoutRef,
            hashMapOf<String, Any>(
                "teacherId" to teacherId,
                "teacherName" to teacherName,
                "amount" to amount,
                "reference" to reference,
                "receiptUrl" to receiptUrl,
                "paidBy" to adminUid,
                "paidByName" to adminName,
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to now
            )
        )
        batch.set(
            notificationRef,
            hashMapOf<String, Any>(
                "userId" to teacherId,
                "type" to "payout",
                "title" to "Payment received",
                "message" to "${rs(amount)} was sent to your bank account." +
                        if (reference.isNotBlank()) " Reference: $reference" else "",
                "amount" to amount,
                "receiptUrl" to receiptUrl,
                "payoutId" to payoutRef.id,
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to now
            )
        )
        batch.commit().await()
    }

    private suspend fun uploadReceipt(uri: Uri, fileName: String): String {
        val context = FirebaseApp.getInstance().applicationContext
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return CloudinaryUploader
            .uploadFile(context, uri, "${System.currentTimeMillis()}_$safeName", "nena_sayura/receipts")
            .getOrThrow()
    }

    private fun DocumentSnapshot.toPayout() = Payout(
        id = id,
        teacherId = getString("teacherId") ?: "",
        teacherName = getString("teacherName") ?: "",
        amount = getDouble("amount") ?: 0.0,
        reference = getString("reference") ?: "",
        receiptUrl = getString("receiptUrl") ?: "",
        paidByName = getString("paidByName") ?: "",
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            ?: getLong("createdAtMillis")
            ?: 0L
    )

    private companion object {
        const val PAYOUTS = "payouts"
        const val NOTIFICATIONS = "notifications"
    }
}