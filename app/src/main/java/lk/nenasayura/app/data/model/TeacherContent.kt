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
    val videoUrl: String = "",
    val createdAtMillis: Long = 0L
) {
    /** The link a student (or the teacher) opens: the video link, or the uploaded file. */
    val openUrl: String get() = if (type == ContentType.VIDEO_LINK) videoUrl else fileUrl
}