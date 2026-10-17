package lk.nenasayura.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.local.ReminderPreferencesStore

/**
 * Alarms are lost when the phone restarts, the app is updated or the time / time zone changes.
 * This sets them again from the settings and sessions saved in DataStore.
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                val app = context.applicationContext
                val pending = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        if (ReminderPreferencesStore(app).loadPreferences().enabled) {
                            ReminderScheduler.reschedule(app)
                            ReminderSyncWorker.enqueuePeriodic(app)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}