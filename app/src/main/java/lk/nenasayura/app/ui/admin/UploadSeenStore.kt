package lk.nenasayura.app.ui.admin

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lk.nenasayura.app.data.model.ContentStatus
import lk.nenasayura.app.data.model.TeacherContent

/**
 * Remembers, on this phone, which teacher uploads the admin has already looked at.
 *
 * For every teacher it saves the time of the newest upload the admin has seen.
 * An upload newer than that is "new": it shows a NEW badge until the admin opens that teacher.
 * When the teacher uploads again, the badge comes back.
 */
object UploadSeenStore {

    private const val PREFS = "admin_seen_uploads"

    private var prefs: SharedPreferences? = null
    private val _seen = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** teacherId -> time (millis) of the newest upload the admin has seen. */
    val seen: StateFlow<Map<String, Long>> = _seen.asStateFlow()

    /** Call once with any Context before reading. Safe to call many times. */
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _seen.value = p.all.mapNotNull { (key, value) -> (value as? Long)?.let { key to it } }.toMap()
    }

    /** The admin opened this teacher: everything up to [uploadMillis] counts as seen. */
    fun markSeen(teacherId: String, uploadMillis: Long) {
        if ((_seen.value[teacherId] ?: 0L) >= uploadMillis) return
        prefs?.edit()?.putLong(teacherId, uploadMillis)?.apply()
        _seen.update { it + (teacherId to uploadMillis) }
    }

    /** Pending uploads (they need a decision) plus uploads the admin has not looked at yet. */
    fun attentionCount(contents: List<TeacherContent>, seen: Map<String, Long>): Int =
        contents.count {
            it.status == ContentStatus.PENDING || it.createdAtMillis > (seen[it.teacherId] ?: 0L)
        }

    /** How many of [contents] the admin has not seen yet. */
    fun newCount(contents: List<TeacherContent>, seen: Map<String, Long>): Int =
        contents.count { it.createdAtMillis > (seen[it.teacherId] ?: 0L) }
}