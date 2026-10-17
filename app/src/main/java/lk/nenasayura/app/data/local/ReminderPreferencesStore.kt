package lk.nenasayura.app.data.local



import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import lk.nenasayura.app.data.model.ReminderPreferences
import lk.nenasayura.app.data.model.ReminderSession
import lk.nenasayura.app.data.model.SessionStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

// One DataStore for the whole app process (there must be only one per file name)
private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "nena_reminders")

/**
 * Reminder settings (ON/OFF, lead time, daily time) in DataStore Preferences.
 * It also keeps a small copy of the next timetable sessions and the ids of the alarms
 * that are scheduled, so the alarms can be set again offline and after a reboot.
 * No Room database is used.
 */
class ReminderPreferencesStore(context: Context) {

    private val dataStore: DataStore<Preferences> = context.applicationContext.reminderDataStore

    private val data: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    // ---------- settings ----------

    /** Emits the settings now and every time they change. */
    val preferencesFlow: Flow<ReminderPreferences> = data
        .map { it.toReminderPreferences() }
        .distinctUntilChanged()

    suspend fun loadPreferences(): ReminderPreferences = preferencesFlow.first()

    /** Reads the current settings, applies [transform] and saves the result. */
    suspend fun updatePreferences(transform: (ReminderPreferences) -> ReminderPreferences) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toReminderPreferences())
            prefs[Keys.ENABLED] = updated.enabled
            prefs[Keys.SESSION_ENABLED] = updated.sessionRemindersEnabled
            prefs[Keys.LEAD_MINUTES] = updated.leadMinutes
            prefs[Keys.DAILY_ENABLED] = updated.dailyEnabled
            prefs[Keys.DAILY_MINUTE] = updated.dailyMinuteOfDay
        }
    }

    // ---------- cached sessions ----------

    val sessionsFlow: Flow<List<ReminderSession>> = data
        .map { parseSessions(it[Keys.SESSIONS]) }
        .distinctUntilChanged()

    suspend fun loadSessions(): List<ReminderSession> = sessionsFlow.first()

    suspend fun saveSessions(ownerUid: String, sessions: List<ReminderSession>) {
        val array = JSONArray()
        sessions.forEach { array.put(it.toJson()) }
        dataStore.edit { prefs ->
            prefs[Keys.SESSIONS] = array.toString()
            prefs[Keys.OWNER] = ownerUid
        }
    }

    /** The student whose sessions are cached. */
    suspend fun cacheOwner(): String? = data.first()[Keys.OWNER]

    suspend fun clearSessions() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SESSIONS)
            prefs.remove(Keys.OWNER)
        }
    }

    // ---------- scheduled alarm ids ----------

    suspend fun loadScheduledIds(): Set<String> {
        val raw = data.first()[Keys.SCHEDULED] ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getString(it) }.toSet()
        } catch (e: JSONException) {
            emptySet()
        }
    }

    suspend fun saveScheduledIds(ids: Set<String>) {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        dataStore.edit { it[Keys.SCHEDULED] = array.toString() }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    // ---------- mapping ----------

    private fun Preferences.toReminderPreferences(): ReminderPreferences {
        val defaults = ReminderPreferences()
        return ReminderPreferences(
            enabled = this[Keys.ENABLED] ?: defaults.enabled,
            sessionRemindersEnabled = this[Keys.SESSION_ENABLED] ?: defaults.sessionRemindersEnabled,
            leadMinutes = this[Keys.LEAD_MINUTES] ?: defaults.leadMinutes,
            dailyEnabled = this[Keys.DAILY_ENABLED] ?: defaults.dailyEnabled,
            dailyMinuteOfDay = this[Keys.DAILY_MINUTE] ?: defaults.dailyMinuteOfDay
        )
    }

    private fun parseSessions(raw: String?): List<ReminderSession> {
        if (raw == null) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.toSession() }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun ReminderSession.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("date", date)
        .put("startMinute", startMinute)
        .put("durationMinutes", durationMinutes)
        .put("title", title)
        .put("subtitle", subtitle)
        .put("status", status.key)

    private fun JSONObject.toSession(): ReminderSession? {
        val id = optString("id")
        val date = optString("date")
        if (id.isBlank() || date.isBlank()) return null
        return ReminderSession(
            id = id,
            date = date,
            startMinute = optInt("startMinute", 0),
            durationMinutes = optInt("durationMinutes", 45),
            title = optString("title"),
            subtitle = optString("subtitle"),
            status = SessionStatus.fromKey(optString("status"))
        )
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val SESSION_ENABLED = booleanPreferencesKey("session_enabled")
        val LEAD_MINUTES = intPreferencesKey("lead_minutes")
        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val DAILY_MINUTE = intPreferencesKey("daily_minute")
        val SESSIONS = stringPreferencesKey("cached_sessions")
        val OWNER = stringPreferencesKey("cache_owner")
        val SCHEDULED = stringPreferencesKey("scheduled_ids")
    }
}