package lk.nenasayura.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.local.ReminderPreferencesStore
import java.time.LocalDate

/** Receives the AlarmManager alarms and shows the notifications. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val action = intent.action ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)
        val text = intent.getStringExtra(ReminderScheduler.EXTRA_TEXT)
        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)

        // DataStore is read asynchronously, so keep the receiver alive until we are done
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(app, action, title, text, notificationId)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(
        app: Context,
        action: String,
        title: String?,
        text: String?,
        notificationId: Int
    ) {
        val store = ReminderPreferencesStore(app)
        val prefs = store.loadPreferences()
        if (!prefs.enabled) return

        when (action) {
            ReminderScheduler.ACTION_SESSION -> {
                if (!prefs.sessionRemindersEnabled) return
                if (title == null || text == null) return
                ReminderNotifications.show(app, notificationId, title, text)
            }

            ReminderScheduler.ACTION_DAILY -> {
                if (!prefs.dailyEnabled) return
                // Set tomorrow's alarm first, so a problem below never stops the daily reminder
                ReminderScheduler.scheduleDaily(app, prefs.dailyMinuteOfDay)
                val (dailyTitle, dailyText) = ReminderMessages.daily(
                    sessions = store.loadSessions(),
                    minuteOfDay = prefs.dailyMinuteOfDay,
                    today = LocalDate.now()
                )
                ReminderNotifications.show(app, DAILY_NOTIFICATION_ID, dailyTitle, dailyText)
            }
        }
    }

    private companion object {
        const val DAILY_NOTIFICATION_ID = 7001
    }
}