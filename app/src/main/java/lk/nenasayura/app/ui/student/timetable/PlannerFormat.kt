package lk.nenasayura.app.ui.student.timetable



import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SHORT_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val FULL_DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Day number 1 (Monday) to 7 (Sunday) as "Mon". */
fun shortDayName(day: Int): String = SHORT_DAYS.getOrElse(day - 1) { "" }

fun fullDayName(day: Int): String = FULL_DAYS.getOrElse(day - 1) { "" }

/** 90 -> "1 h 30 min", 120 -> "2 h", 45 -> "45 min". */
fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}

/** Minutes after midnight as a clock time: 1110 -> "6:30 pm". */
fun formatClock(minuteOfDay: Int): String {
    val hour24 = minuteOfDay / 60
    val minute = minuteOfDay % 60
    val suffix = if (hour24 >= 12) "pm" else "am"
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, suffix)
}

/** The date picker gives the chosen day as UTC midnight in milliseconds; this turns it back into a date. */
fun examLocalDate(examDateMillis: Long): LocalDate =
    Instant.ofEpochMilli(examDateMillis).atZone(ZoneOffset.UTC).toLocalDate()

fun formatExamDate(examDateMillis: Long): String =
    examLocalDate(examDateMillis).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

/** Days from today to the exam (negative when the date has passed). */
fun daysUntilExam(examDateMillis: Long): Long =
    ChronoUnit.DAYS.between(LocalDate.now(), examLocalDate(examDateMillis))