package lk.nenasayura.app.core.planner



import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import java.time.LocalDate
import kotlin.math.roundToInt

enum class LoadSuggestion { NONE, LIGHTER, FULLER }

/** How one subject did in the reviewed week. */
data class SubjectReview(
    val subjectId: String,
    val subjectName: String,
    val done: Int,
    val missed: Int
) {
    val total: Int get() = done + missed
    val rate: Double get() = if (total == 0) 0.0 else done.toDouble() / total

    /** Sessions kept being skipped, so this subject gets a little more time. */
    val needsMoreTime: Boolean get() = total >= 2 && (rate < 0.5 || missed >= 2)
}

/** The result of looking back at one week. */
data class WeekReview(
    val weekStart: LocalDate,
    val done: Int,
    /** Missed sessions, including the ones that were never marked. */
    val missed: Int,
    /** Past sessions that were never marked done or missed. */
    val unmarked: Int,
    val subjects: List<SubjectReview>,
    /** Missed sessions that will be planned again. */
    val carryOver: List<StudySession>,
    /** The week before's done rate, or null when it had too few sessions. */
    val previousRate: Double?,
    val suggestion: LoadSuggestion
) {
    val total: Int get() = done + missed
    val rate: Double get() = if (total == 0) 0.0 else done.toDouble() / total
}

/**
 * The weekly review rules. Pure Kotlin, so it is easy to test and easy to explain:
 *  - only the student's planned sessions count (their own custom sessions do not)
 *  - a past session that was never marked counts as missed
 *  - missed sessions are carried over (at most [MAX_CARRY_OVER])
 *  - a subject that is skipped a lot gets a small extra weight, which fades at each review
 *  - after a very busy or very easy stretch, lighter or fuller days are suggested
 */
object WeekReviewer {

    const val MAX_CARRY_OVER = 6

    private const val MIN_SESSIONS_FOR_SUGGESTION = 4
    private const val WEIGHT_STEP = 0.15
    private const val MIN_WEIGHT = 0.7
    private const val MAX_WEIGHT = 1.5
    private const val MAX_DAY_MINUTES = 480

    /**
     * @param thisWeek     the sessions of the week being reviewed
     * @param previousWeek the sessions of the week before it (can be empty)
     */
    fun review(
        weekStart: LocalDate,
        thisWeek: List<StudySession>,
        previousWeek: List<StudySession>,
        today: LocalDate
    ): WeekReview {
        val counted = thisWeek.filter { counts(it, today) }
        val done = counted.count { it.status == SessionStatus.DONE }
        val notDone = counted.filter { it.status != SessionStatus.DONE }

        val subjects = counted.groupBy { it.subjectId }
            .map { (id, list) ->
                SubjectReview(
                    subjectId = id,
                    subjectName = list.first().subjectName,
                    done = list.count { it.status == SessionStatus.DONE },
                    missed = list.count { it.status != SessionStatus.DONE }
                )
            }
            .sortedBy { it.subjectName }

        val previous = previousWeek.filter { counts(it, today) }
        val previousRate = if (previous.size >= MIN_SESSIONS_FOR_SUGGESTION) {
            previous.count { it.status == SessionStatus.DONE }.toDouble() / previous.size
        } else null

        val rate = if (counted.isEmpty()) 0.0 else done.toDouble() / counted.size

        return WeekReview(
            weekStart = weekStart,
            done = done,
            missed = notDone.size,
            unmarked = counted.count { it.status == SessionStatus.PLANNED },
            subjects = subjects,
            carryOver = notDone.sortedWith(compareBy({ it.date }, { it.startMinute })).take(MAX_CARRY_OVER),
            previousRate = previousRate,
            suggestion = suggest(rate, counted.size, previousRate)
        )
    }

    // Done, missed, and past sessions that were never marked. Custom and future sessions do not count.
    private fun counts(session: StudySession, today: LocalDate): Boolean =
        session.kind != SessionKind.CUSTOM &&
                (session.status != SessionStatus.PLANNED || LocalDate.parse(session.date).isBefore(today))

    private fun suggest(rate: Double, total: Int, previousRate: Double?): LoadSuggestion = when {
        total < MIN_SESSIONS_FOR_SUGGESTION -> LoadSuggestion.NONE
        rate < 0.5 -> LoadSuggestion.LIGHTER
        rate < 0.6 && previousRate != null && previousRate < 0.6 -> LoadSuggestion.LIGHTER
        rate >= 0.9 && previousRate != null && previousRate >= 0.9 -> LoadSuggestion.FULLER
        else -> LoadSuggestion.NONE
    }

    /** New subject multipliers. Old ones fade by half; skipped subjects get a small boost. */
    fun adjustWeights(current: Map<String, Double>, review: WeekReview): Map<String, Double> {
        val boosted = review.subjects.filter { it.needsMoreTime }.map { it.subjectId }.toSet()
        val ids = current.keys + review.subjects.map { it.subjectId }
        return ids.associateWith { id ->
            val faded = 1.0 + ((current[id] ?: 1.0) - 1.0) * 0.5
            val boost = if (id in boosted) WEIGHT_STEP else 0.0
            ((faded + boost).coerceIn(MIN_WEIGHT, MAX_WEIGHT) * 100).roundToInt() / 100.0
        }
    }

    /** One session lighter (or fuller) on every study day, if the student accepted the suggestion. */
    fun adjustLoad(settings: StudyPlanSettings, suggestion: LoadSuggestion): StudyPlanSettings {
        if (suggestion == LoadSuggestion.NONE) return settings
        val length = settings.sessionMinutes
        val updated = settings.dailyMinutes.mapValues { (_, minutes) ->
            val count = PlanGenerator.sessionsThatFit(minutes, length)
            when {
                count == 0 -> minutes
                suggestion == LoadSuggestion.LIGHTER ->
                    minOf(minutes, PlanGenerator.minutesNeeded(maxOf(1, count - 1), length))
                else ->
                    maxOf(minutes, minOf(PlanGenerator.minutesNeeded(count + 1, length), MAX_DAY_MINUTES))
            }
        }
        return settings.copy(dailyMinutes = updated)
    }
}