package lk.nenasayura.app.core.planner



import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.LearningStyle
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.StudyGoal
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.data.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** How close the exam is. It decides the mix of session types. */
enum class StudyPhase(val label: String) {
    BUILD("Build"),        // more than 120 days left
    PRACTICE("Practice"),  // 30 to 120 days
    REVISE("Revise"),      // 8 to 29 days
    EXAM_WEEK("Exam week"); // 7 days or less

    companion object {
        fun forDaysToExam(days: Long): StudyPhase = when {
            days > 120 -> BUILD
            days >= 30 -> PRACTICE
            days >= 8 -> REVISE
            else -> EXAM_WEEK
        }
    }
}

/**
 * Builds a personal study timetable from the student's settings.
 * Pure Kotlin: no Android, no Firestore, so it is easy to test.
 * The same input always gives the same plan.
 */
object PlanGenerator {

    const val DEFAULT_WEEKS = 4

    private const val BREAK_MINUTES = 10
    private const val LONG_BREAK_MINUTES = 20
    private const val FLEX_MINUTES = 20          // free time at the end of each day
    private const val MINUTES_IN_DAY = 24 * 60
    private const val MAX_SAME_SUBJECT_PER_DAY = 2
    private const val EXAM_WEEK_MAX_SESSIONS = 2

    /** The first day AFTER the planned period. */
    fun horizonEnd(startDate: LocalDate, weeks: Int = DEFAULT_WEEKS): LocalDate =
        startDate.plusDays(7L * weeks)

    /**
     * @param subjects the student's subjects (from the profile)
     * @param seed something unique to the student (their uid), so equal answers still give different plans
     */
    fun generate(
        settings: StudyPlanSettings,
        subjects: List<Subject>,
        startDate: LocalDate,
        seed: String,
        weeks: Int = DEFAULT_WEEKS
    ): List<StudySession> {
        if (subjects.isEmpty() || settings.availableDays.isEmpty()) return emptyList()

        // The date picker stores the exam day as UTC midnight
        val examDate: LocalDate? = if (settings.examDateMillis > 0L) {
            Instant.ofEpochMilli(settings.examDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
        } else null

        val slots = buildSlots(settings, startDate, weeks, examDate)
        if (slots.isEmpty()) return emptyList()

        val subjectForSlot = assignSubjects(slots, subjects, settings, seed)
        val kindForSlot = assignKinds(slots, subjectForSlot, settings, examDate)

        return slots.indices.map { i ->
            StudySession(
                date = slots[i].date.toString(),
                startMinute = slots[i].startMinute,
                durationMinutes = settings.sessionMinutes,
                subjectId = subjectForSlot[i].id,
                subjectName = subjectForSlot[i].name,
                kind = kindForSlot[i]
            )
        }
    }

    // ---------- step 1: the time slots ----------

    private data class Slot(val date: LocalDate, val startMinute: Int, val indexInDay: Int)

    private fun breakAfter(sessionNumber: Int): Int =
        if (sessionNumber % 2 == 0) LONG_BREAK_MINUTES else BREAK_MINUTES

    /** Minutes needed for [count] sessions of [length] with their breaks (and the flex time at the end). */
    private fun minutesUsed(count: Int, length: Int, withFlex: Boolean): Int {
        val breaks = (1 until count).sumOf { breakAfter(it) }
        return count * length + breaks + if (withFlex) FLEX_MINUTES else 0
    }

    private fun sessionsThatFit(dayMinutes: Int, length: Int): Int {
        var count = 0
        while (minutesUsed(count + 1, length, withFlex = true) <= dayMinutes) count++
        // A day that fits one session but not the flex time still gets that session
        if (count == 0 && dayMinutes >= length) count = 1
        return count
    }

    private fun buildSlots(
        settings: StudyPlanSettings,
        startDate: LocalDate,
        weeks: Int,
        examDate: LocalDate?
    ): List<Slot> {
        var end = horizonEnd(startDate, weeks)
        if (examDate != null && examDate.isBefore(end)) end = examDate // nothing on the exam day

        val length = settings.sessionMinutes
        val slots = mutableListOf<Slot>()
        var date = startDate
        while (date.isBefore(end)) {
            val dayNumber = date.dayOfWeek.value // 1 = Monday ... 7 = Sunday
            if (dayNumber in settings.availableDays) {
                var count = sessionsThatFit(settings.dailyMinutes[dayNumber] ?: 0, length)
                if (examDate != null && ChronoUnit.DAYS.between(date, examDate) <= 7) {
                    count = minOf(count, EXAM_WEEK_MAX_SESSIONS)
                }
                if (count > 0) {
                    // Start at the preferred time, but early enough to finish before midnight
                    val total = minutesUsed(count, length, withFlex = true)
                    var cursor = maxOf(0, minOf(settings.preferredWindow.startMinute, MINUTES_IN_DAY - total))
                    for (i in 0 until count) {
                        slots.add(Slot(date, cursor, i))
                        cursor += length + breakAfter(i + 1)
                    }
                }
            }
            date = date.plusDays(1)
        }
        return slots
    }

    // ---------- step 2: which subject in which slot ----------

    // A "smooth weighted round robin": every slot, each subject earns credit equal to its weight;
    // the subject with the most credit is picked and pays the total weight. This spreads subjects
    // evenly while giving weaker subjects proportionally more slots.
    private fun assignSubjects(
        slots: List<Slot>,
        subjects: List<Subject>,
        settings: StudyPlanSettings,
        seed: String
    ): List<Subject> {
        val weight = subjects.associate { it.id to Confidence.fromLevel(settings.confidence[it.id]).weight }
        val totalWeight = weight.values.sum()
        // A tiny starting credit from the seed makes different students start differently
        val credit = subjects.associate { it.id to tieBreaker(seed, it.id) }.toMutableMap()
        val countToday = mutableMapOf<String, Int>()
        val result = ArrayList<Subject>(slots.size)

        slots.forEachIndexed { index, slot ->
            if (index == 0 || slot.date != slots[index - 1].date) countToday.clear()
            subjects.forEach { credit[it.id] = credit.getValue(it.id) + weight.getValue(it.id) }

            val previousId = if (slot.indexInDay > 0) result.last().id else null
            val ranked = subjects.sortedByDescending { credit.getValue(it.id) }
            // Best subject that is not the previous one and is under today's limit;
            // if none exists (very long days, few subjects), relax the rules step by step
            val pick = ranked.firstOrNull { (countToday[it.id] ?: 0) < MAX_SAME_SUBJECT_PER_DAY && it.id != previousId }
                ?: ranked.firstOrNull { (countToday[it.id] ?: 0) < MAX_SAME_SUBJECT_PER_DAY }
                ?: ranked.first()

            credit[pick.id] = credit.getValue(pick.id) - totalWeight
            countToday[pick.id] = (countToday[pick.id] ?: 0) + 1
            result.add(pick)
        }
        return result
    }

    private fun tieBreaker(seed: String, subjectId: String): Double =
        (((seed + subjectId).hashCode() and 0x7fffffff) % 1000) / 2000.0

    // ---------- step 3: what kind of session ----------

    private enum class Category { LEARN, QUIZ, PAPER, REVISION }

    private val baseMix: Map<StudyPhase, Map<Category, Int>> = mapOf(
        StudyPhase.BUILD to mapOf(Category.LEARN to 50, Category.QUIZ to 20, Category.PAPER to 10, Category.REVISION to 20),
        StudyPhase.PRACTICE to mapOf(Category.LEARN to 30, Category.QUIZ to 25, Category.PAPER to 25, Category.REVISION to 20),
        StudyPhase.REVISE to mapOf(Category.LEARN to 10, Category.QUIZ to 25, Category.PAPER to 35, Category.REVISION to 30),
        StudyPhase.EXAM_WEEK to mapOf(Category.LEARN to 0, Category.QUIZ to 20, Category.PAPER to 20, Category.REVISION to 60)
    )

    /** The phase's mix (percentages that add up to 100), tilted by the student's goal and style. */
    private fun mixFor(phase: StudyPhase, goal: StudyGoal, style: LearningStyle): Map<Category, Int> {
        val mix = baseMix.getValue(phase).toMutableMap()
        fun move(from: Category, to: Category, amount: Int) {
            val moved = minOf(amount, mix.getValue(from))
            mix[from] = mix.getValue(from) - moved
            mix[to] = mix.getValue(to) + moved
        }
        if (phase != StudyPhase.EXAM_WEEK) {
            when (goal) {
                StudyGoal.UNDERSTAND -> move(Category.PAPER, Category.LEARN, 10)
                StudyGoal.EXAM_READY -> move(Category.LEARN, Category.PAPER, 10)
                StudyGoal.MARKS -> Unit
            }
            if (style == LearningStyle.PRACTICE) move(Category.LEARN, Category.QUIZ, 10)
        }
        return mix
    }

    /** The order learning sessions rotate through, depending on the learning style. */
    private fun learnKindsFor(style: LearningStyle): List<SessionKind> = when (style) {
        LearningStyle.VIDEO -> listOf(SessionKind.LEARN_VIDEO, SessionKind.LEARN_NOTE, SessionKind.LEARN_VIDEO, SessionKind.LEARN_BOOK)
        LearningStyle.READING -> listOf(SessionKind.LEARN_NOTE, SessionKind.LEARN_BOOK, SessionKind.LEARN_NOTE, SessionKind.LEARN_VIDEO)
        LearningStyle.PRACTICE -> listOf(SessionKind.LEARN_NOTE, SessionKind.LEARN_VIDEO)
        LearningStyle.BALANCED -> listOf(SessionKind.LEARN_VIDEO, SessionKind.LEARN_NOTE, SessionKind.LEARN_BOOK)
    }

    private fun assignKinds(
        slots: List<Slot>,
        subjectForSlot: List<Subject>,
        settings: StudyPlanSettings,
        examDate: LocalDate?
    ): List<SessionKind> {
        val learnOrder = learnKindsFor(settings.learningStyle)
        val credit = Category.entries.associateWith { 0.0 }.toMutableMap()
        val learned = mutableSetOf<String>()        // subjects that already had a learning session
        val needsReview = mutableSetOf<String>()    // subjects that just did a paper
        var learnRotation = 0
        val kinds = ArrayList<SessionKind>(slots.size)

        slots.forEachIndexed { index, slot ->
            val phase = if (examDate != null) {
                StudyPhase.forDaysToExam(ChronoUnit.DAYS.between(slot.date, examDate))
            } else StudyPhase.PRACTICE
            val mix = mixFor(phase, settings.goal, settings.learningStyle)
            Category.entries.forEach { credit[it] = credit.getValue(it) + mix.getValue(it) }

            val previous = if (slot.indexInDay > 0) kinds.last() else null
            val subjectId = subjectForSlot[index].id

            // The next learning kind in the rotation, skipping the one used just before
            fun nextLearn(): Pair<SessionKind, Int> {
                for (offset in learnOrder.indices) {
                    val position = (learnRotation + offset) % learnOrder.size
                    if (learnOrder[position] != previous) return learnOrder[position] to position + 1
                }
                return learnOrder[learnRotation % learnOrder.size] to learnRotation + 1
            }

            val category: Category
            val kind: SessionKind
            if (subjectId in needsReview && previous != SessionKind.MARKING_REVIEW) {
                // Right after a paper comes the marking scheme review
                category = Category.PAPER
                kind = SessionKind.MARKING_REVIEW
                needsReview.remove(subjectId)
            } else if (subjectId !in learned && mix.getValue(Category.LEARN) > 0) {
                // The first session of every subject is learning
                category = Category.LEARN
                val (learnKind, nextRotation) = nextLearn()
                kind = learnKind
                learnRotation = nextRotation
            } else {
                val candidates = Category.entries
                    .filter { mix.getValue(it) > 0 }
                    .map { c ->
                        when (c) {
                            Category.LEARN -> {
                                val (learnKind, nextRotation) = nextLearn()
                                Triple(c, learnKind, nextRotation)
                            }
                            Category.QUIZ -> Triple(c, SessionKind.QUIZ, learnRotation)
                            Category.PAPER -> Triple(c, SessionKind.PAPER, learnRotation)
                            Category.REVISION -> Triple(c, SessionKind.REVISION, learnRotation)
                        }
                    }
                // Never the same kind twice in a row on one day
                val allowed = candidates.filter { it.second != previous }.ifEmpty { candidates }
                val best = allowed.maxByOrNull { credit.getValue(it.first) } ?: candidates.first()
                category = best.first
                kind = best.second
                learnRotation = best.third
            }

            credit[category] = credit.getValue(category) - 100.0
            if (category == Category.LEARN) learned.add(subjectId)
            if (kind == SessionKind.PAPER) needsReview.add(subjectId)
            kinds.add(kind)
        }
        return kinds
    }
}