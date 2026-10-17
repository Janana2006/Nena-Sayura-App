package lk.nenasayura.app.ui.student.reminders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.reminders.ReminderNotifications
import lk.nenasayura.app.core.reminders.ReminderScheduler
import lk.nenasayura.app.core.reminders.ReminderSyncWorker
import lk.nenasayura.app.core.reminders.ReminderSyncer
import lk.nenasayura.app.data.local.ReminderPreferencesStore
import lk.nenasayura.app.data.model.ReminderPreferences
import lk.nenasayura.app.data.model.UpcomingReminder

data class RemindersUiState(
    /** false until DataStore has delivered the saved settings. */
    val isLoaded: Boolean = false,
    val prefs: ReminderPreferences = ReminderPreferences(),
    /** The reminders that are waiting to fire, soonest first. */
    val upcoming: List<UpcomingReminder> = emptyList(),
    /** false = no timetable sessions are saved on the phone yet. */
    val hasTimetableData: Boolean = false,
    val isSyncing: Boolean = false
)

class RemindersViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Application = application
    private val store = ReminderPreferencesStore(application)

    private val isSyncing = MutableStateFlow(false)
    private var syncJob: Job? = null

    val uiState: StateFlow<RemindersUiState> = combine(
        store.preferencesFlow,
        store.sessionsFlow,
        isSyncing
    ) { prefs, sessions, syncing ->
        RemindersUiState(
            isLoaded = true,
            prefs = prefs,
            upcoming = ReminderScheduler.upcoming(prefs, sessions, System.currentTimeMillis())
                .take(MAX_LISTED),
            hasTimetableData = sessions.isNotEmpty(),
            isSyncing = syncing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    init {
        // So "Study reminders" already shows in the phone's notification settings
        ReminderNotifications.ensureChannel(application)
    }

    /** Called when the screen appears or comes back: refreshes the timetable copy and the alarms. */
    fun refresh() {
        viewModelScope.launch {
            if (store.loadPreferences().enabled) startSync()
        }
    }

    // ---------- settings (saved in DataStore, then the alarms are set again) ----------

    fun setEnabled(enabled: Boolean) {
        change { it.copy(enabled = enabled) }
        if (enabled) {
            ReminderSyncWorker.enqueuePeriodic(appContext)
            startSync()
        } else {
            ReminderSyncWorker.cancel(appContext)
        }
    }

    fun setSessionRemindersEnabled(enabled: Boolean) = change { it.copy(sessionRemindersEnabled = enabled) }

    fun setLeadMinutes(minutes: Int) = change { it.copy(leadMinutes = minutes) }

    fun setDailyEnabled(enabled: Boolean) = change { it.copy(dailyEnabled = enabled) }

    fun setDailyTime(minuteOfDay: Int) =
        change { it.copy(dailyMinuteOfDay = minuteOfDay.coerceIn(0, 24 * 60 - 1)) }

    // ---------- internals ----------

    private fun change(transform: (ReminderPreferences) -> ReminderPreferences) {
        viewModelScope.launch {
            store.updatePreferences(transform)
            ReminderScheduler.reschedule(appContext) // works offline, from the saved sessions
        }
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = viewModelScope.launch {
            isSyncing.value = true
            try {
                ReminderSyncer.sync(appContext)
            } finally {
                isSyncing.value = false
            }
        }
    }

    private companion object {
        const val MAX_LISTED = 30
    }
}