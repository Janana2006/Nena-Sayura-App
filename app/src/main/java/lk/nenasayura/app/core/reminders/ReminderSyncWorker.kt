package lk.nenasayura.app.core.reminders

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Refreshes the timetable copy and the alarms in the background, even when the app is closed,
 * so a new or changed timetable gets its reminders too.
 */
class ReminderSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ReminderSyncer.sync(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "reminder_sync_periodic"
        private const val ONCE_NAME = "reminder_sync_once"

        /** Every 6 hours, when there is internet. */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** One refresh now (for example after the timetable changed). Quick no-op when reminders are off. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReminderSyncWorker>().build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            val manager = WorkManager.getInstance(context.applicationContext)
            manager.cancelUniqueWork(PERIODIC_NAME)
            manager.cancelUniqueWork(ONCE_NAME)
        }
    }
}