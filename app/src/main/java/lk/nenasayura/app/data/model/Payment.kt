package lk.nenasayura.app.data.model

/**
 * Represents a student's payment record for a teacher or course.
 * Saved in Firestore collection "payments".
 */
data class Payment(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val amount: Double = 0.0,
    val status: String = "paid", // e.g. "paid", "pending", "failed"
    val commissionPercent: Double? = null,
    val createdAtMillis: Long = 0L
) {
    val isPaid: Boolean
        get() = status.equals("paid", ignoreCase = true) || status.equals("success", ignoreCase = true)

    val isPending: Boolean
        get() = status.equals("pending", ignoreCase = true)
}