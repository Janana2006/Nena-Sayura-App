package lk.nenasayura.app.core.reminders



import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import lk.nenasayura.app.R
import lk.nenasayura.app.data.model.ReminderSession
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.ui.student.timetable.formatClock
import lk.nenasayura.app.ui.theme.NenaPrimary
import java.time.LocalDate

/** The notification channel, permission checks and showing a notification. */
object ReminderNotifications {

    const val CHANNEL_ID = "study_reminders"

    /** Creates the "Study reminders" channel (safe to call many times). */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders before your study sessions and your daily study summary"
        }
        manager.createNotificationChannel(channel)
    }

    /** True when the app is allowed to show notifications (permission + not blocked in Settings). */
    fun areNotificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Opens this app's notification settings page. */
    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No settings screen to open
        }
    }

    /** Shows one reminder notification. Does nothing when notifications are not allowed. */
    fun show(context: Context, notificationId: Int, title: String, text: String) {
        if (!areNotificationsAllowed(context)) return
        ensureChannel(context)

        // Tapping the notification opens the app
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(NenaPrimary.toArgb())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission was removed while we were working
        }
    }
}

/** The words used in the notifications. */
object ReminderMessages {

    fun sessionTitle(leadMinutes: Int): String = "Study session in $leadMinutes min"

    fun sessionText(session: ReminderSession): String =
        "${session.title} • ${session.subtitle}, " +
                "${formatClock(session.startMinute)} - ${formatClock(session.startMinute + session.durationMinutes)}"

    /**
     * Daily summary. Before 3 pm it is the plan for today, later it is today's progress.
     * Returns title to text.
     */
    fun daily(sessions: List<ReminderSession>, minuteOfDay: Int, today: LocalDate): Pair<String, String> {
        val todays = sessions.filter { it.date == today.toString() }.sortedBy { it.startMinute }
        val total = todays.size
        val morning = minuteOfDay < 15 * 60

        if (morning) {
            val title = "Today's study plan"
            if (total == 0) return title to "No study sessions are planned today. Open your Timetable to add one."
            val first = todays.firstOrNull { it.status == SessionStatus.PLANNED } ?: todays.first()
            val plural = if (total == 1) "session" else "sessions"
            return title to "You have $total study $plural today. First: ${first.title} at ${formatClock(first.startMinute)}."
        }

        val title = "Today's study progress"
        if (total == 0) return title to "Nothing was planned today. Plan tomorrow's study in your Timetable."
        val done = todays.count { it.status == SessionStatus.DONE }
        val left = total - done
        return if (left == 0) {
            title to "Great job! You finished all $total sessions today."
        } else {
            title to "$done of $total sessions done today. $left still to go - you can still catch up!"
        }
    }
}