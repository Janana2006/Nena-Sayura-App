package lk.nenasayura.app.data.model

/** What kind of material a teacher uploads. */
enum class ContentType(val key: String, val label: String) {
    SHORT_NOTE("short_note", "Short Note"),
    TUTE("tute", "Tute"),
    MODEL_PAPER("model_paper", "Model Paper"),
    NOTE("note", "Note"),
    VIDEO_LINK("video_link", "Video Link");

    companion object {
        fun fromKey(key: String?): ContentType =
            entries.firstOrNull { it.key == key } ?: NOTE
    }
}

/** Who can open the material. FREE = every student, PAID = only students who paid this teacher. */
enum class AccessType(val key: String, val label: String) {
    FREE("free", "Free"),
    PAID("paid", "Paid");

    companion object {
        fun fromKey(key: String?): AccessType =
            entries.firstOrNull { it.key == key } ?: FREE
    }
}

/**
 * Review state of an upload.
 * PENDING  = waiting for the admin (students cannot see it),
 * APPROVED = visible to students,
 * DECLINED = rejected by the admin (the teacher sees the reason).
 * Old documents that have no "status" field count as APPROVED, so nothing disappears.
 */
enum class ContentStatus(val key: String, val label: String) {
    PENDING("pending", "Pending"),
    APPROVED("approved", "Approved"),
    DECLINED("declined", "Declined");

    companion object {
        fun fromKey(key: String?): ContentStatus =
            entries.firstOrNull { it.key == key } ?: APPROVED
    }
}

/** One item uploaded by a teacher (Firestore collection "teacher_content"). */
data class TeacherContent(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val title: String = "",
    val description: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val type: ContentType = ContentType.NOTE,
    val access: AccessType = AccessType.FREE,
    val fileUrl: String = "",
    val fileName: String = "",
    val storagePath: String = "",
    val answerFileUrl: String = "",
    val answerFileName: String = "",
    val answerStoragePath: String = "",
    val videoUrl: String = "",
    val status: ContentStatus = ContentStatus.APPROVED,
    /** The message the admin wrote when declining (empty otherwise). */
    val declineReason: String = "",
    val reviewedAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L
) {
    /** The link a student (or the teacher) opens: the video link, or the uploaded file. */
    val openUrl: String get() = if (type == ContentType.VIDEO_LINK) videoUrl else fileUrl
}