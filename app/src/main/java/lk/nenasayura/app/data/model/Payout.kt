package lk.nenasayura.app.data.model

/**
 * One payment the admin made TO a teacher (the teacher's share of the student payments).
 * Saved in Firestore collection "payouts". A payout is never edited or deleted, so the list
 * works as a permanent history. What is still to pay = teacher's share - the sum of payouts.
 */
data class Payout(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val amount: Double = 0.0,
    /** Bank transfer reference number (optional). */
    val reference: String = "",
    /** Link of the receipt photo / PDF (Cloudinary). */
    val receiptUrl: String = "",
    val paidByName: String = "",
    val createdAtMillis: Long = 0L
) {
    val hasReceipt: Boolean get() = receiptUrl.isNotBlank()
}