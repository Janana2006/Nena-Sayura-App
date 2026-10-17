package lk.nenasayura.app.core.planner

import junit.framework.Assert.assertTrue
import lk.nenasayura.app.data.model.LearningStyle
import lk.nenasayura.app.data.model.PreferredWindow
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.StudyGoal
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.data.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class PlanGeneratorTest {

    private val subjects = listOf(
        Subject(id = "chem", name = "Chemistry"),
        Subject(id = "phys", name = "Physics"),
        Subject(id = "math", name = "Combined Maths")
    )

    // 19 October 2026 is a Monday
    private val monday = LocalDate.of(2026, 10, 19)

    private val learningKinds = setOf(
        SessionKind.LEARN_VIDEO, SessionKind.LEARN_NOTE, SessionKind.LEARN_BOOK
    )

    private fun millis(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    // Mon-Fri 2 hours, Saturday 4 hours, Sunday rest; 45-minute sessions in the evening
    private fun sampleSettings(
        examInDays: Long = 130,
        confidence: Map<String, Int> = mapOf("chem" to 1, "phys" to 2, "math" to 3)
    ) = StudyPlanSettings(
        confidence = confidence,
        availableDays = listOf(1, 2, 3, 4, 5, 6),
        dailyMinutes = mapOf(1 to 120, 2 to 120, 3 to 120, 4 to 120, 5 to 120, 6 to 240),
        preferredWindow = PreferredWindow.EVENING,
        sessionMinutes = 45,
        learningStyle = LearningStyle.VIDEO,
        goal = StudyGoal.MARKS,
        examDateMillis = millis(monday.plusDays(examInDays))
    )

    private fun plan(settings: StudyPlanSettings = sampleSettings(), seed: String = "student-a"): List<StudySession> =
        PlanGenerator.generate(settings, subjects, monday, seed)

    @Test
    fun onlyChosenDaysAreUsedAndTheFirstWeekHasFourteenSessions() {
        val sessions = plan()
        assertTrue(sessions.none { LocalDate.parse(it.date).dayOfWeek.value == 7 })
        assertEquals(14, sessions.count { LocalDate.parse(it.date) < monday.plusDays(7) })
    }

    @Test
    fun theWeakestSubjectGetsTheMostSessions() {
        val counts = plan().groupingBy { it.subjectId }.eachCount()
        assertTrue(counts.getValue("chem") > counts.getValue("phys"))
        assertTrue(counts.getValue("phys") > counts.getValue("math"))
    }

    @Test
    fun theSameKindNeverRepeatsBackToBackOnADay() {
        plan().groupBy { it.date }.values.forEach { day ->
            day.zipWithNext().forEach { (a, b) -> assertNotEquals(a.kind, b.kind) }
        }
    }

    @Test
    fun aSubjectHasAtMostTwoSessionsOnOneDay() {
        plan().groupBy { it.date }.values.forEach { day ->
            assertTrue(day.groupingBy { it.subjectId }.eachCount().values.all { it <= 2 })
        }
    }

    @Test
    fun sessionsStayInsideTheDayAndDoNotOverlap() {
        plan().groupBy { it.date }.values.forEach { day ->
            day.forEach { assertTrue(it.startMinute >= 0 && it.startMinute + it.durationMinutes <= 24 * 60) }
            day.zipWithNext().forEach { (a, b) ->
                assertTrue(b.startMinute >= a.startMinute + a.durationMinutes)
            }
        }
    }

    @Test
    fun theFirstSessionOfEverySubjectIsLearning() {
        val sessions = plan()
        subjects.forEach { subject ->
            assertTrue(sessions.first { it.subjectId == subject.id }.kind in learningKinds)
        }
    }

    @Test
    fun aMarkingReviewAlwaysFollowsAPaperOfTheSameSubject() {
        val sessions = plan()
        subjects.forEach { subject ->
            var papersWaiting = 0
            sessions.filter { it.subjectId == subject.id }.forEach {
                if (it.kind == SessionKind.PAPER) papersWaiting++
                if (it.kind == SessionKind.MARKING_REVIEW) {
                    assertTrue(papersWaiting > 0)
                    papersWaiting--
                }
            }
        }
    }

    @Test
    fun noSessionsOnOrAfterTheExamDate() {
        val exam = monday.plusDays(10)
        val sessions = plan(sampleSettings(examInDays = 10))
        assertTrue(sessions.isNotEmpty())
        assertTrue(sessions.all { LocalDate.parse(it.date).isBefore(exam) })
    }

    @Test
    fun examWeekHasAtMostTwoSessionsADay() {
        val exam = monday.plusDays(12)
        val sessions = plan(sampleSettings(examInDays = 12))
        sessions.groupBy { it.date }.forEach { (date, day) ->
            if (ChronoUnit.DAYS.between(LocalDate.parse(date), exam) <= 7) assertTrue(day.size <= 2)
        }
    }

    @Test
    fun theSameStudentAlwaysGetsTheSamePlanButDifferentStudentsDiffer() {
        val equal = sampleSettings(confidence = mapOf("chem" to 2, "phys" to 2, "math" to 2))
        val first = plan(equal, "student-a")
        assertEquals(first, plan(equal, "student-a"))
        assertNotEquals(
            first.map { it.subjectId },
            plan(equal, "student-b").map { it.subjectId }
        )
    }

    @Test
    fun noSubjectsGivesNoSessions() {
        assertTrue(PlanGenerator.generate(sampleSettings(), emptyList(), monday, "x").isEmpty())
    }

    @Test
    fun thePlanCoversFourWeeks() {
        assertEquals(monday.plusDays(28), PlanGenerator.horizonEnd(monday))
    }

    @Test
    fun aWeightAdjustmentGivesThatSubjectMoreSessions() {
        val equal = sampleSettings(confidence = mapOf("chem" to 2, "phys" to 2, "math" to 2))
            .copy(weightAdjustment = mapOf("chem" to 1.5))
        val counts = plan(equal).groupingBy { it.subjectId }.eachCount()
        assertTrue(counts.getValue("chem") > counts.getValue("phys"))
        assertTrue(counts.getValue("chem") > counts.getValue("math"))
    }
}