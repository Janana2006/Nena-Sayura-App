package lk.nenasayura.app.core.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.local.ReminderPreferencesStore
import lk.nenasayura.app.data.model.ReminderPreferences
import lk.nenasayura.app.data.model.ReminderSession
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.UpcomingReminder
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sets and cancels the alarms with AlarmManager. Everything it needs is read from
 * DataStore ([ReminderPreferencesStore]), so it also works offline and after a reboot.
 */
object ReminderScheduler {

    const val ACTION_SESSION = "lk.nenasayura.app.reminder.SESSION"
    const val ACTION_DAILY = "lk.nenasayura.app.reminder.DAILY"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"

    private const val DAILY_KEY = "daily"
    private const val MINUTE_MS = 60_000L

    /** Android allows only a limited number of alarms per app, so only the soonest ones are set. */
    private const val MAX_SESSION_ALARMS = 60

    /** For work that must finish even when the screen that started it is gone. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---------- what is coming up ----------

    /** The session reminders that have not fired yet, soonest first. */
    fun upcoming(
        prefs: ReminderPreferences,
        sessions: List<ReminderSession>,
        now: Long
    ): List<UpcomingReminder> {
        if (!prefs.enabled || !prefs.sessionRemindersEnabled) return emptyList()
        return sessions
            .filter { it.status == SessionStatus.PLANNED }
            .map { UpcomingReminder(it, it.startMillis - prefs.leadMinutes * MINUTE_MS, prefs.leadMinutes) }
            .filter { it.triggerMillis > now }
            .sortedBy { it.triggerMillis }
    }

    // ---------- scheduling ----------

    /** Cancels the old alarms and sets new ones from the saved settings and sessions. */
    suspend fun reschedule(context: Context) {
        val app = context.applicationContext
        val store = ReminderPreferencesStore(app)
        cancelAlarms(app, store)

        val prefs = store.loadPreferences()
        if (!prefs.enabled) return

        val scheduledIds = mutableSetOf<String>()
        if (prefs.sessionRemindersEnabled) {
            upcoming(prefs, store.loadSessions(), System.currentTimeMillis())
                .take(MAX_SESSION_ALARMS)
                .forEach { reminder ->
                    val session = reminder.session
                    val intent = alarmIntent(app, ACTION_SESSION, sessionKey(session.id))
                        .putExtra(EXTRA_NOTIFICATION_ID, session.id.hashCode())
                        .putExtra(EXTRA_TITLE, ReminderMessages.sessionTitle(reminder.leadMinutes))
                        .putExtra(EXTRA_TEXT, ReminderMessages.sessionText(session))
                    setAlarm(app, reminder.triggerMillis, broadcast(app, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                    scheduledIds += session.id
                }
        }
        store.saveScheduledIds(scheduledIds)

        if (prefs.dailyEnabled) scheduleDaily(app, prefs.dailyMinuteOfDay)
    }

    /** Sets the next daily reminder (today if the time is still ahead, otherwise tomorrow). */
    fun scheduleDaily(context: Context, minuteOfDay: Int) {
        val app = context.applicationContext
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var next = now.toLocalDate()
            .atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
            .atZone(zone)
        if (!next.isAfter(now.plusSeconds(30))) next = next.plusDays(1)

        val intent = alarmIntent(app, ACTION_DAILY, DAILY_KEY)
        setAlarm(app, next.toInstant().toEpochMilli(), broadcast(app, intent, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    /** Cancels every alarm and forgets everything (used when the student signs out). */
    suspend fun cancelAndClear(context: Context) {
        val app = context.applicationContext
        val store = ReminderPreferencesStore(app)
        cancelAlarms(app, store)
        store.clearAll()
        ReminderSyncWorker.cancel(app)
    }

    /** Same as [cancelAndClear], for callers that are not suspend functions (for example a button click). */
    fun cancelAndClearAsync(context: Context) {
        val app = context.applicationContext
        appScope.launch { cancelAndClear(app) }
    }

    // ---------- exact alarm permission ----------

    /** Before Android 12 exact alarms are always allowed; later the student must allow them. */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No settings screen to open
        }
    }

    // ---------- helpers ----------

    private fun sessionKey(sessionId: String) = "session/${Uri.encode(sessionId)}"

    /** The same action + data always gives the same PendingIntent, so an alarm can be found and cancelled. */
    private fun alarmIntent(context: Context, action: String, key: String): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("nena://reminder/$key"))

    private fun broadcast(context: Context, intent: Intent, flag: Int): PendingIntent =
        PendingIntent.getBroadcast(context, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)

    private suspend fun cancelAlarms(context: Context, store: ReminderPreferencesStore) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return

        fun cancel(action: String, key: String) {
            val pending = PendingIntent.getBroadcast(
                context, 0, alarmIntent(context, action, key),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                manager.cancel(pending)
                pending.cancel()
            }
        }

        store.loadScheduledIds().forEach { cancel(ACTION_SESSION, sessionKey(it)) }
        cancel(ACTION_DAILY, DAILY_KEY)
        store.saveScheduledIds(emptySet())
    }

    private fun setAlarm(context: Context, triggerAtMillis: Long, pending: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            if (canScheduleExactAlarms(context)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                // Not allowed to be exact: Android may delay it by a few minutes
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
        } catch (e: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }
}