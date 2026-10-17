package lk.nenasayura.app.data.repository



import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.LearningStyle
import lk.nenasayura.app.data.model.PreferredWindow
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudyGoal
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import java.util.Date

/**
 * Saves and loads one student's study planner in Firestore:
 *   users/{uid}/study_plan/settings   the answers (one document)
 *   users/{uid}/study_sessions/{id}   one document per study session
 *
 * Note: a write waits for the server, so with no internet it can wait a long time.
 * The screens that call it should use a timeout (like the Write review screen does).
 */
class StudyPlanRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun settingsRef(uid: String) =
        db.collection(USERS).document(uid).collection(PLAN).document(SETTINGS)

    private fun sessionsRef(uid: String) =
        db.collection(USERS).document(uid).collection(SESSIONS)

    // ---------- settings ----------

    /** The student's planner answers, or null when they have not set up a plan yet. */
    suspend fun getSettings(uid: String): StudyPlanSettings? {
        val doc = settingsRef(uid).get().await()
        return if (doc.exists()) doc.toSettings() else null
    }

    /** Saves (replaces) the planner answers. */
    suspend fun saveSettings(uid: String, settings: StudyPlanSettings) {
        val data = hashMapOf<String, Any>(
            "confidence" to settings.confidence,
            "availableDays" to settings.availableDays,
            // Firestore map keys must be text
            "dailyMinutes" to settings.dailyMinutes.mapKeys { it.key.toString() },
            "preferredWindow" to settings.preferredWindow.key,
            "sessionMinutes" to settings.sessionMinutes,
            "learningStyle" to settings.learningStyle.key,
            "goal" to settings.goal.key,
            "generatedUntil" to settings.generatedUntil,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (settings.examDateMillis > 0L) {
            data["examDate"] = Timestamp(Date(settings.examDateMillis))
        }
        settingsRef(uid).set(data).await()
    }

    // ---------- sessions ----------

    /** Sessions from [fromDate] to [toDate] (both like "2026-10-19"), in time order. */
    suspend fun getSessions(uid: String, fromDate: String, toDate: String): List<StudySession> {
        // Both conditions are on the same field, so no Firestore index is needed
        val snapshot = sessionsRef(uid)
            .whereGreaterThanOrEqualTo("date", fromDate)
            .whereLessThanOrEqualTo("date", toDate)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toSession() }
            .sortedWith(compareBy({ it.date }, { it.startMinute }))
    }

    /** Adds new sessions (new ids are made here). Batches are limited to 500 writes, so they are split. */
    suspend fun addSessions(uid: String, sessions: List<StudySession>) {
        sessions.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { session -> batch.set(sessionsRef(uid).document(), session.toMap()) }
            batch.commit().await()
        }
    }

    /** Deletes the sessions from [fromDate] on that are still PLANNED. Done and missed ones stay as history. */
    suspend fun deletePlannedFrom(uid: String, fromDate: String) {
        val snapshot = sessionsRef(uid)
            .whereGreaterThanOrEqualTo("date", fromDate)
            .get()
            .await()
        val planned = snapshot.documents.filter { it.getString("status") == SessionStatus.PLANNED.key }
        planned.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    /** Marks a session done or missed (or back to planned). */
    suspend fun updateStatus(uid: String, sessionId: String, status: SessionStatus) {
        val completed = if (status == SessionStatus.DONE) System.currentTimeMillis() else 0L
        sessionsRef(uid).document(sessionId).update(
            mapOf(
                "status" to status.key,
                "completedAtMillis" to completed
            )
        ).await()
    }

    /** Moves a session to another day or time. It stays planned and remembers where it came from. */
    suspend fun moveSession(uid: String, session: StudySession, newDate: String, newStartMinute: Int) {
        sessionsRef(uid).document(session.id).update(
            mapOf(
                "date" to newDate,
                "startMinute" to newStartMinute,
                "status" to SessionStatus.PLANNED.key,
                "originalDate" to session.originalDate.ifBlank { session.date }
            )
        ).await()
    }

    /** Deletes one session (for example a custom session the student no longer wants). */
    suspend fun deleteSession(uid: String, sessionId: String) {
        sessionsRef(uid).document(sessionId).delete().await()
    }

    // ---------- Firestore <-> models ----------
    // If the field names are changed, change them here only.

    private fun StudySession.toMap(): HashMap<String, Any> = hashMapOf(
        "date" to date,
        "startMinute" to startMinute,
        "durationMinutes" to durationMinutes,
        "subjectId" to subjectId,
        "subjectName" to subjectName,
        "kind" to kind.key,
        "resourceType" to resourceType,
        "resourceId" to resourceId,
        "resourceTitle" to resourceTitle,
        "status" to status.key,
        "originalDate" to originalDate,
        "completedAtMillis" to completedAtMillis
    )

    private fun DocumentSnapshot.toSession(): StudySession? {
        val date = getString("date") ?: return null
        return StudySession(
            id = id,
            date = date,
            startMinute = (getLong("startMinute") ?: 0L).toInt(),
            durationMinutes = (getLong("durationMinutes") ?: 45L).toInt(),
            subjectId = getString("subjectId") ?: "",
            subjectName = getString("subjectName") ?: "",
            kind = SessionKind.fromKey(getString("kind")),
            resourceType = getString("resourceType") ?: "",
            resourceId = getString("resourceId") ?: "",
            resourceTitle = getString("resourceTitle") ?: "",
            status = SessionStatus.fromKey(getString("status")),
            originalDate = getString("originalDate") ?: "",
            completedAtMillis = getLong("completedAtMillis") ?: 0L
        )
    }

    private fun DocumentSnapshot.toSettings(): StudyPlanSettings {
        val confidence = (get("confidence") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val subjectId = key as? String ?: return@mapNotNull null
                val level = (value as? Number)?.toInt() ?: return@mapNotNull null
                subjectId to Confidence.fromLevel(level).level
            }?.toMap() ?: emptyMap()

        val days = (get("availableDays") as? List<*>)
            ?.mapNotNull { (it as? Number)?.toInt() }
            ?.filter { it in 1..7 }
            ?: emptyList()

        val minutes = (get("dailyMinutes") as? Map<*, *>)
            ?.mapNotNull { (key, value) ->
                val day = key?.toString()?.toIntOrNull() ?: return@mapNotNull null
                val mins = (value as? Number)?.toInt() ?: return@mapNotNull null
                day to mins
            }?.toMap() ?: emptyMap()

        return StudyPlanSettings(
            confidence = confidence,
            availableDays = days,
            dailyMinutes = minutes,
            preferredWindow = PreferredWindow.fromKey(getString("preferredWindow")),
            sessionMinutes = (getLong("sessionMinutes") ?: 45L).toInt(),
            learningStyle = LearningStyle.fromKey(getString("learningStyle")),
            goal = StudyGoal.fromKey(getString("goal")),
            examDateMillis = (get("examDate") as? Timestamp)?.toDate()?.time ?: 0L,
            generatedUntil = getString("generatedUntil") ?: ""
        )
    }

    private companion object {
        const val USERS = "users"
        const val PLAN = "study_plan"
        const val SETTINGS = "settings"
        const val SESSIONS = "study_sessions"
        const val BATCH_SIZE = 400
    }
}