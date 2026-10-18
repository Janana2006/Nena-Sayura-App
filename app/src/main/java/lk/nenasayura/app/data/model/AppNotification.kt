package lk.nenasayura.app.data.model

/** A notification for one user, saved in Firestore collection "notifications". */
data class AppNotification(
    val id: String = "",
    val userId: String = "",
    /** For example "payout" (the admin paid the teacher). */
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val amount: Double = 0.0,
    val receiptUrl: String = "",
    val read: Boolean = false,
    val createdAtMillis: Long = 0L
) {
    val hasReceipt: Boolean get() = receiptUrl.isNotBlank()
}