package lk.nenasayura.app.data.model

/** One payment a student made to a teacher (Firestore "payments"). */
data class Payment(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val subjectName: String = "",
    val amount: Double = 0.0,
    /** "paid" = student has access, "pending" = waiting, "refunded" = access removed. */
    val status: String = "",
    /** Admin commission % saved at payment time (null = use the current default). */
    val commissionPercent: Double? = null,
    val createdAtMillis: Long = 0L
) {
    val isPaid: Boolean get() = status.equals("paid", ignoreCase = true)
    val isPending: Boolean get() = status.equals("pending", ignoreCase = true)
}