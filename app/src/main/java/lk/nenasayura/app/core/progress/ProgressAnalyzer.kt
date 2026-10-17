package lk.nenasayura.app.core.progress



import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.QuizAttempt
import lk.nenasayura.app.data.model.Subject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How well the student is doing in a subject right now. */
enum class Level(val label: String) {
    NOT_STARTED("Not started"),
    NEEDS_WORK("Needs work"),
    GETTING_THERE("Getting there"),
    STRONG("Strong")
}

/** Are the latest quiz results better or worse than the earlier ones? */
enum class Trend(val label: String) {
    NEW("Not enough data"),
    UP("Improving"),
    STEADY("Steady"),
    DOWN("Slipping")
}

/** One lesson quiz inside a subject. */
data class QuizProgress(
    val quizId: String,
    val title: String,
    val attempts: Int,
    val firstPercent: Int,
    val latestPercent: Int,
    val bestPercent: Int,
    val lastAttemptMillis: Long
) {
    val improvement: Int get() = latestPercent - firstPercent
    val needsRevision: Boolean get() = latestPercent < ProgressAnalyzer.REVISE_BELOW
}

data class SubjectProgress(
    val subjectId: String,
    val name: String,
    val attempts: Int,
    val questionsAnswered: Int,
    val correctAnswers: Int,
    /** Recency-weighted score (0-100): the latest attempts count the most. */
    val currentPercent: Int,
    val level: Level,
    val trend: Trend,
    /** Recent average minus earlier average, in percentage points. */
    val trendDelta: Int,
    val lastAttemptMillis: Long,
    val quizzesAttempted: Int,
    val quizzesAvailable: Int,
    /** Weakest lesson first. */
    val quizzes: List<QuizProgress>
) {
    val accuracy: Int
        get() = if (questionsAnswered == 0) 0 else correctAnswers * 100 / questionsAnswered

    /** With only 1-2 attempts the level is a rough guess. */
    val isReliable: Boolean get() = attempts >= ProgressAnalyzer.RELIABLE_ATTEMPTS
}

data class OverallProgress(
    val totalAttempts: Int,
    val quizzesAttempted: Int,
    val quizzesAvailable: Int,
    val questionsAnswered: Int,
    val correctAnswers: Int,
    /** Average of the last 5 attempts. */
    val recentAveragePercent: Int,
    val bestPercent: Int,
    val streakDays: Int,
    val attemptsLast7Days: Int,
    val daysSinceLastAttempt: Int?
) {
    val accuracy: Int
        get() = if (questionsAnswered == 0) 0 else correctAnswers * 100 / questionsAnswered
}

/** A point on the "last attempts" chart. */
data class AttemptPoint(val percent: Int, val millis: Long, val title: String)

/** A short tip. When [quizId] is set the screen can open that quiz directly. */
data class Recommendation(
    val title: String,
    val detail: String,
    val quizId: String? = null
)

data class ProgressReport(
    val overall: OverallProgress,
    val subjects: List<SubjectProgress>,
    /** Oldest first, at most the last 10 attempts. */
    val recentAttempts: List<AttemptPoint>,
    val recommendations: List<Recommendation>
) {
    val hasData: Boolean get() = overall.totalAttempts > 0
}

/**
 * Turns the saved quiz attempts into a study-progress report.
 * Pure Kotlin (no Firebase, no Android), so it is easy to unit test.
 */
object ProgressAnalyzer {

    const val REVISE_BELOW = 60
    const val STRONG_FROM = 80
    const val GETTING_THERE_FROM = 55
    const val RELIABLE_ATTEMPTS = 3
    private const val RECENCY_WEIGHT = 0.5      // the newest attempt counts 50%, the one before 25%...
    private const val TREND_STEP = 5            // points of change before we call it up/down
    private const val INACTIVE_DAYS = 5
    private const val MAX_TIPS = 4

    /**
     * @param attempts every saved attempt (any order)
     * @param quizzes every quiz in the app (used for "how many quizzes are still untried")
     * @param subjects every subject (used for names)
     * @param mySubjectIds the student's own subjects; they are always listed, even with no attempts
     */
    fun analyze(
        attempts: List<QuizAttempt>,
        quizzes: List<Quiz>,
        subjects: List<Subject>,
        mySubjectIds: List<String>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): ProgressReport {
        val sorted = attempts.sortedBy { it.createdAtMillis }          // oldest -> newest
        val subjectNames = subjects.associate { it.id to it.name }

        // Subjects to show: the student's own first, then any other subject they have attempts in
        val subjectIds = (mySubjectIds + sorted.map { it.subjectId })
            .filter { it.isNotBlank() }
            .distinct()

        val subjectReports = subjectIds.map { id ->
            buildSubject(
                subjectId = id,
                name = subjectNames[id] ?: id,
                attempts = sorted.filter { it.subjectId == id },
                subjectQuizzes = quizzes.filter { it.subjectId == id }
            )
        }
            // Needs-work subjects first, then the ones with no data, then the strong ones
            .sortedWith(
                compareBy<SubjectProgress> { levelOrder(it.level) }
                    .thenBy { it.currentPercent }
                    .thenBy { it.name }
            )

        val overall = buildOverall(sorted, quizzes, mySubjectIds, today, zone)

        val recent = sorted.takeLast(10).map {
            AttemptPoint(percent = it.percent, millis = it.createdAtMillis, title = it.quizTitle)
        }

        return ProgressReport(
            overall = overall,
            subjects = subjectReports,
            recentAttempts = recent,
            recommendations = buildTips(subjectReports, overall)
        )
    }

    // ---------- subject ----------

    private fun buildSubject(
        subjectId: String,
        name: String,
        attempts: List<QuizAttempt>,           // oldest -> newest
        subjectQuizzes: List<Quiz>
    ): SubjectProgress {
        if (attempts.isEmpty()) {
            return SubjectProgress(
                subjectId = subjectId,
                name = name,
                attempts = 0,
                questionsAnswered = 0,
                correctAnswers = 0,
                currentPercent = 0,
                level = Level.NOT_STARTED,
                trend = Trend.NEW,
                trendDelta = 0,
                lastAttemptMillis = 0L,
                quizzesAttempted = 0,
                quizzesAvailable = subjectQuizzes.size,
                quizzes = emptyList()
            )
        }

        val percents = attempts.map { it.percent.toDouble() }
        val current = recencyWeighted(percents).toInt()
        val (trend, delta) = trendOf(percents)

        val quizReports = attempts
            .groupBy { it.quizId }
            .map { (quizId, quizAttempts) ->
                QuizProgress(
                    quizId = quizId,
                    title = quizAttempts.last().quizTitle.ifBlank { "Quiz" },
                    attempts = quizAttempts.size,
                    firstPercent = quizAttempts.first().percent,
                    latestPercent = quizAttempts.last().percent,
                    bestPercent = quizAttempts.maxOf { it.percent },
                    lastAttemptMillis = quizAttempts.last().createdAtMillis
                )
            }
            .sortedBy { it.latestPercent }

        return SubjectProgress(
            subjectId = subjectId,
            name = name,
            attempts = attempts.size,
            questionsAnswered = attempts.sumOf { it.total },
            correctAnswers = attempts.sumOf { it.score },
            currentPercent = current,
            level = levelOf(current),
            trend = trend,
            trendDelta = delta,
            lastAttemptMillis = attempts.last().createdAtMillis,
            quizzesAttempted = quizReports.size,
            quizzesAvailable = maxOf(subjectQuizzes.size, quizReports.size),
            quizzes = quizReports
        )
    }

    /** Exponential moving average: the latest attempts matter the most. */
    private fun recencyWeighted(percents: List<Double>): Double {
        var value = percents.first()
        for (index in 1 until percents.size) {
            value = RECENCY_WEIGHT * percents[index] + (1 - RECENCY_WEIGHT) * value
        }
        return value
    }

    /** Compares the latest few attempts with everything before them. */
    private fun trendOf(percents: List<Double>): Pair<Trend, Int> {
        if (percents.size < 2) return Trend.NEW to 0
        val window = minOf(3, percents.size / 2)
        val recent = percents.takeLast(window).average()
        val earlier = percents.dropLast(window).average()
        val delta = (recent - earlier).toInt()
        val trend = when {
            delta >= TREND_STEP -> Trend.UP
            delta <= -TREND_STEP -> Trend.DOWN
            else -> Trend.STEADY
        }
        return trend to delta
    }

    private fun levelOf(percent: Int): Level = when {
        percent >= STRONG_FROM -> Level.STRONG
        percent >= GETTING_THERE_FROM -> Level.GETTING_THERE
        else -> Level.NEEDS_WORK
    }

    private fun levelOrder(level: Level): Int = when (level) {
        Level.NEEDS_WORK -> 0
        Level.NOT_STARTED -> 1
        Level.GETTING_THERE -> 2
        Level.STRONG -> 3
    }

    // ---------- overall ----------

    private fun buildOverall(
        sorted: List<QuizAttempt>,
        quizzes: List<Quiz>,
        mySubjectIds: List<String>,
        today: LocalDate,
        zone: ZoneId
    ): OverallProgress {
        val days: Set<LocalDate> = sorted
            .map { Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate() }
            .toSet()

        val mine = mySubjectIds.toSet()
        val available = if (mine.isEmpty()) quizzes else quizzes.filter { it.subjectId in mine }

        return OverallProgress(
            totalAttempts = sorted.size,
            quizzesAttempted = sorted.map { it.quizId }.distinct().size,
            quizzesAvailable = available.size,
            questionsAnswered = sorted.sumOf { it.total },
            correctAnswers = sorted.sumOf { it.score },
            recentAveragePercent = if (sorted.isEmpty()) 0 else sorted.takeLast(5).map { it.percent }.average().toInt(),
            bestPercent = sorted.maxOfOrNull { it.percent } ?: 0,
            streakDays = streak(days, today),
            attemptsLast7Days = sorted.count {
                val day = Instant.ofEpochMilli(it.createdAtMillis).atZone(zone).toLocalDate()
                !day.isBefore(today.minusDays(6)) && !day.isAfter(today)
            },
            daysSinceLastAttempt = days.maxOrNull()?.let {
                java.time.temporal.ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0)
            }
        )
    }

    /** Days in a row with at least one quiz. Today may still be empty without breaking the streak. */
    private fun streak(days: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    // ---------- tips ----------

    private fun buildTips(subjects: List<SubjectProgress>, overall: OverallProgress): List<Recommendation> {
        val tips = mutableListOf<Recommendation>()

        if (overall.totalAttempts == 0) {
            tips += Recommendation(
                title = "Take your first quiz",
                detail = "Finish one quiz and your progress report will start to fill in."
            )
            return tips
        }

        // 1. The subjects that need work (lowest first), pointing at their weakest lesson
        subjects.filter { it.level == Level.NEEDS_WORK }.sortedBy { it.currentPercent }.forEach { subject ->
            val weakest = subject.quizzes.firstOrNull { it.needsRevision }
            tips += Recommendation(
                title = "Revise ${subject.name}",
                detail = if (weakest != null) {
                    "Your recent level is ${subject.currentPercent}%. Retry \"${weakest.title}\" - your last score was ${weakest.latestPercent}%."
                } else {
                    "Your recent level is ${subject.currentPercent}%. Try another quiz to lift it."
                },
                quizId = weakest?.quizId
            )
        }

        // 2. Subjects that are slipping, even if they are not "needs work" yet
        subjects.filter { it.trend == Trend.DOWN && it.level != Level.NEEDS_WORK }.forEach { subject ->
            tips += Recommendation(
                title = "${subject.name} is slipping",
                detail = "Your latest quizzes are ${-subject.trendDelta} points lower than before. A quick revision will help.",
                quizId = subject.quizzes.firstOrNull()?.quizId
            )
        }

        // 3. Subjects with no quiz at all
        subjects.filter { it.level == Level.NOT_STARTED && it.quizzesAvailable > 0 }.forEach { subject ->
            tips += Recommendation(
                title = "Start ${subject.name}",
                detail = "You have not tried any of its ${subject.quizzesAvailable} quizzes yet."
            )
        }

        // 4. Not practising lately
        val idle = overall.daysSinceLastAttempt
        if (idle != null && idle >= INACTIVE_DAYS) {
            tips += Recommendation(
                title = "Get back to practice",
                detail = "It has been $idle days since your last quiz. Even one short quiz keeps things fresh."
            )
        }

        // 5. Everything is fine: point at untried quizzes, or praise
        if (tips.isEmpty()) {
            val untried = subjects.firstOrNull { it.quizzesAvailable > it.quizzesAttempted }
            tips += if (untried != null) {
                Recommendation(
                    title = "Keep it going",
                    detail = "You are doing well. ${untried.name} still has ${untried.quizzesAvailable - untried.quizzesAttempted} quizzes you have not tried."
                )
            } else {
                Recommendation(
                    title = "Great work",
                    detail = "You are strong in every subject you have practised. Retry a quiz to beat your best score."
                )
            }
        }

        return tips.take(MAX_TIPS)
    }
}