package lk.nenasayura.app.data.model

/** A student's review of the app, stored in the Firestore "reviews" collection. */
data class Review(
    val id: String = "",
    val studentName: String = "",
    /** 1 to 5 stars */
    val rating: Int = 0,
    val comment: String = "",
    /** Time the review was written, in milliseconds (0 when the document has no date). */
    val createdAtMillis: Long = 0L
)