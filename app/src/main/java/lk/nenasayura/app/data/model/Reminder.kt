package lk.nenasayura.app.data.model



import java.time.LocalDate
import java.time.ZoneId

/** Lead-time choices (minutes before a session) shown in the Reminders screen. */
val REMINDER_LEAD_OPTIONS: List<Int> = listOf(5, 10, 15, 30)

/** The student's reminder settings. Saved on the phone (SharedPreferences). */
data class ReminderPreferences(
    /** Master switch: when false nothing is scheduled. */
    val enabled: Boolean = false,
    /** Notify before each planned timetable session. */
    val sessionRemindersEnabled: Boolean = true,
    /** One of [REMINDER_LEAD_OPTIONS]. */
    val leadMinutes: Int = 15,
    /** Daily summary / planning reminder. */
    val dailyEnabled: Boolean = false,
    /** Time of the daily reminder, in minutes after midnight (default 9:00 pm). */
    val dailyMinuteOfDay: Int = 21 * 60
)

/** A small copy of a timetable session kept on the phone, so reminders work offline. */
data class ReminderSession(
    val id: String,
    /** Like "2026-10-19". */
    val date: String,
    val startMinute: Int,
    val durationMinutes: Int,
    val title: String,
    val subtitle: String,
    val status: SessionStatus
) {
    /** The start of the session in epoch milliseconds (device time zone). */
    val startMillis: Long
        get() = LocalDate.parse(date)
            .atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(startMinute.toLong())
            .toInstant()
            .toEpochMilli()
}

/** One reminder that is waiting to fire. */
data class UpcomingReminder(
    val session: ReminderSession,
    val triggerMillis: Long,
    val leadMinutes: Int
)

fun StudySession.toReminderSession(): ReminderSession = ReminderSession(
    id = id,
    date = date,
    startMinute = startMinute,
    durationMinutes = durationMinutes,
    title = if (kind == SessionKind.CUSTOM) {
        resourceTitle.ifBlank { "My session" }
    } else {
        subjectName.ifBlank { kind.label }
    },
    subtitle = kind.label,
    status = status
)