package lk.nenasayura.app.core.reminders

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import lk.nenasayura.app.data.local.ReminderPreferencesStore
import lk.nenasayura.app.data.model.toReminderSession
import lk.nenasayura.app.data.repository.StudyPlanRepository
import java.time.LocalDate

/**
 * Reads the next days of the student's timetable from the same [StudyPlanRepository]
 * the Timetable screen uses, keeps a copy in DataStore and sets the AlarmManager alarms.
 * If there is no network the alarms are set from the copy already on the phone.
 */
object ReminderSyncer {

    private const val HORIZON_DAYS = 14L
    private const val TIMEOUT_MS = 10_000L

    /** @return true when the timetable was refreshed, false when the saved copy was used. */
    suspend fun sync(context: Context): Boolean {
        val app = context.applicationContext
        val store = ReminderPreferencesStore(app)

        if (!store.loadPreferences().enabled) {
            ReminderScheduler.reschedule(app) // cancels everything
            return true
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            // Signed out: nothing should fire
            ReminderScheduler.cancelAndClear(app)
            return false
        }

        // The saved copy belongs to another student: drop it
        if (store.cacheOwner() != uid) store.clearSessions()

        return try {
            val today = LocalDate.now()
            val sessions = withTimeout(TIMEOUT_MS) {
                StudyPlanRepository().getSessions(uid, today.toString(), today.plusDays(HORIZON_DAYS).toString())
            }
            store.saveSessions(uid, sessions.map { it.toReminderSession() })
            ReminderScheduler.reschedule(app)
            true
        } catch (e: TimeoutCancellationException) {
            ReminderScheduler.reschedule(app)
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ReminderScheduler.reschedule(app)
            false
        }
    }
}