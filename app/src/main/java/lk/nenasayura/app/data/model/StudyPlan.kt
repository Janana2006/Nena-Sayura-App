package lk.nenasayura.app.data.model



import java.time.LocalDate

/** What a study session is about. [key] is what is saved in Firestore. */
enum class SessionKind(val key: String, val label: String) {
    LEARN_VIDEO("learn_video", "Video lesson"),
    LEARN_NOTE("learn_note", "Short note"),
    LEARN_BOOK("learn_book", "Resource book"),
    PAPER("paper", "Paper practice"),
    MARKING_REVIEW("marking_review", "Marking review"),
    QUIZ("quiz", "Quiz"),
    REVISION("revision", "Revision"),
    CUSTOM("custom", "My session");

    companion object {
        fun fromKey(key: String?): SessionKind = entries.firstOrNull { it.key == key } ?: CUSTOM
    }
}

/** A moved session stays PLANNED; it is recognised by its [StudySession.originalDate]. */
enum class SessionStatus(val key: String, val label: String) {
    PLANNED("planned", "Planned"),
    DONE("done", "Done"),
    MISSED("missed", "Missed");

    companion object {
        fun fromKey(key: String?): SessionStatus = entries.firstOrNull { it.key == key } ?: PLANNED
    }
}

/** How well the student feels they know a subject. [level] is saved in Firestore. */
enum class Confidence(val level: Int, val label: String, val weight: Double) {
    STRUGGLING(1, "Struggling", 1.5),
    OK(2, "OK", 1.0),
    STRONG(3, "Strong", 0.7);

    companion object {
        fun fromLevel(level: Int?): Confidence = entries.firstOrNull { it.level == level } ?: OK
    }
}

/** When in the day the student likes to study. [startMinute] = minutes after midnight. */
enum class PreferredWindow(val key: String, val label: String, val startMinute: Int) {
    MORNING("morning", "Morning", 6 * 60),
    AFTERNOON("afternoon", "Afternoon", 13 * 60),
    EVENING("evening", "Evening", 18 * 60 + 30),
    NIGHT("night", "Night", 20 * 60 + 30);

    companion object {
        fun fromKey(key: String?): PreferredWindow = entries.firstOrNull { it.key == key } ?: EVENING
    }
}

enum class LearningStyle(val key: String, val label: String) {
    VIDEO("video", "Videos first"),
    READING("reading", "Reading first"),
    PRACTICE("practice", "Practice first"),
    BALANCED("balanced", "A bit of everything");

    companion object {
        fun fromKey(key: String?): LearningStyle = entries.firstOrNull { it.key == key } ?: BALANCED
    }
}

enum class StudyGoal(val key: String, val label: String) {
    UNDERSTAND("understand", "Build understanding"),
    MARKS("marks", "Improve my marks"),
    EXAM_READY("exam_ready", "Be exam ready");

    companion object {
        fun fromKey(key: String?): StudyGoal = entries.firstOrNull { it.key == key } ?: MARKS
    }
}

/** The student's planner answers. Saved once per student. */
data class StudyPlanSettings(
    /** subjectId -> [Confidence.level] */
    val confidence: Map<String, Int> = emptyMap(),
    /** 1 = Monday ... 7 = Sunday (the same numbers as java.time.DayOfWeek) */
    val availableDays: List<Int> = emptyList(),
    /** day number -> minutes the student can study that day */
    val dailyMinutes: Map<Int, Int> = emptyMap(),
    val preferredWindow: PreferredWindow = PreferredWindow.EVENING,
    /** 30, 45 or 60 */
    val sessionMinutes: Int = 45,
    val learningStyle: LearningStyle = LearningStyle.BALANCED,
    val goal: StudyGoal = StudyGoal.MARKS,
    /** The exam date in milliseconds (0 = not set). */
    val examDateMillis: Long = 0L,
    /** The last date that already has sessions, like "2026-11-15" (empty = no plan yet). */
    val generatedUntil: String = ""
) {
    val hasPlan: Boolean get() = generatedUntil.isNotBlank()
}

/** One study session of the student's timetable. */
data class StudySession(
    val id: String = "",
    /** The day, like "2026-10-19" (this text sorts in date order). */
    val date: String = "",
    /** Start time in minutes after midnight. */
    val startMinute: Int = 0,
    val durationMinutes: Int = 45,
    val subjectId: String = "",
    val subjectName: String = "",
    val kind: SessionKind = SessionKind.CUSTOM,
    /** One of [ResourceTypes], or "" when the session has no resource. */
    val resourceType: String = "",
    val resourceId: String = "",
    /** Title saved for display, so the list does not need extra reads. */
    val resourceTitle: String = "",
    /** The file or video link, saved so a session opens without extra reads ("" for quizzes). */
    val resourceUrl: String = "",
    val status: SessionStatus = SessionStatus.PLANNED,
    /** The day it was planned for before the student moved it ("" = never moved). */
    val originalDate: String = "",
    val completedAtMillis: Long = 0L
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
    val wasMoved: Boolean get() = originalDate.isNotBlank()
}


/** The kinds of resource a session can point to. */
object ResourceTypes {
    const val PAPER = "paper"
    const val MARKING_SCHEME = "marking_scheme"
    const val GUIDE_BOOK = "guide_book"
    const val QUIZ = "quiz"
    const val TEACHER_FILE = "teacher_file"   // a teacher's note, tute or model paper
    const val TEACHER_VIDEO = "teacher_video" // a teacher's video link
}