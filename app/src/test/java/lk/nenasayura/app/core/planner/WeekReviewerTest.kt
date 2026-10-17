package lk.nenasayura.app.core.planner



import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekReviewerTest {

    private val weekStart = LocalDate.of(2026, 10, 5)  // a Monday
    private val today = LocalDate.of(2026, 10, 13)     // the Tuesday after that week

    private var counter = 0

    private fun session(
        status: SessionStatus,
        day: Int = 1,
        subject: String = "chem",
        kind: SessionKind = SessionKind.QUIZ,
        week: LocalDate = weekStart
    ) = StudySession(
        id = "s${counter++}",
        date = week.plusDays((day - 1).toLong()).toString(),
        startMinute = 1110 + counter,
        subjectId = subject,
        subjectName = subject.uppercase(),
        kind = kind,
        status = status
    )

    private fun review(
        thisWeek: List<StudySession>,
        previousWeek: List<StudySession> = emptyList()
    ) = WeekReviewer.review(weekStart, thisWeek, previousWeek, today)

    @Test
    fun countsDoneMissedAndUnmarkedSessions() {
        val result = review(
            listOf(
                session(SessionStatus.DONE), session(SessionStatus.DONE), session(SessionStatus.DONE),
                session(SessionStatus.MISSED), session(SessionStatus.PLANNED)
            )
        )
        assertEquals(3, result.done)
        assertEquals(2, result.missed)
        assertEquals(1, result.unmarked)
        assertEquals(5, result.total)
    }

    @Test
    fun customAndFutureSessionsDoNotCount() {
        val future = StudySession(
            id = "f", date = today.plusDays(3).toString(), subjectId = "chem", subjectName = "CHEM",
            kind = SessionKind.QUIZ, status = SessionStatus.PLANNED
        )
        val result = review(
            listOf(
                session(SessionStatus.DONE, kind = SessionKind.CUSTOM),
                session(SessionStatus.MISSED, kind = SessionKind.CUSTOM),
                future
            )
        )
        assertEquals(0, result.total)
    }

    @Test
    fun missedSessionsAreCarriedOverInTimeOrderAndCapped() {
        val missed = (1..7).map { session(SessionStatus.MISSED, day = it) } +
                session(SessionStatus.DONE, day = 1)
        val result = review(missed)
        assertEquals(WeekReviewer.MAX_CARRY_OVER, result.carryOver.size)
        assertEquals(weekStart.toString(), result.carryOver.first().date)
        assertTrue(result.carryOver.none { it.status == SessionStatus.DONE })
    }

    @Test
    fun suggestsLighterDaysWhenLessThanHalfWasDone() {
        val sessions = listOf(session(SessionStatus.DONE)) + List(5) { session(SessionStatus.MISSED) }
        assertEquals(LoadSuggestion.LIGHTER, review(sessions).suggestion)
    }

    @Test
    fun suggestsFullerDaysOnlyAfterTwoVeryGoodWeeks() {
        val good = List(5) { session(SessionStatus.DONE) }
        val previousGood = List(5) { session(SessionStatus.DONE, week = weekStart.minusWeeks(1)) }
        assertEquals(LoadSuggestion.FULLER, review(good, previousGood).suggestion)
        assertEquals(LoadSuggestion.NONE, review(good).suggestion) // no earlier week to compare with
    }

    @Test
    fun tooFewSessionsGiveNoSuggestion() {
        assertEquals(
            LoadSuggestion.NONE,
            review(listOf(session(SessionStatus.MISSED), session(SessionStatus.MISSED))).suggestion
        )
    }

    @Test
    fun skippedSubjectsGetMoreWeightAndOldWeightsFade() {
        val result = review(List(3) { session(SessionStatus.MISSED, subject = "chem") })
        val adjusted = WeekReviewer.adjustWeights(mapOf("phys" to 1.3), result)
        assertEquals(1.15, adjusted.getValue("chem"), 0.001) // 1.0 + 0.15
        assertEquals(1.15, adjusted.getValue("phys"), 0.001) // 1.3 fades to 1.15
    }

    @Test
    fun weightsNeverGoAboveTheLimit() {
        val result = review(List(3) { session(SessionStatus.MISSED, subject = "chem") })
        val adjusted = WeekReviewer.adjustWeights(mapOf("chem" to 1.5), result)
        assertEquals(1.4, adjusted.getValue("chem"), 0.001) // 1.0 + 0.25 + 0.15
    }

    @Test
    fun lighterAndFullerChangeTheDayLengthByOneSession() {
        val settings = StudyPlanSettings(
            availableDays = listOf(1),
            dailyMinutes = mapOf(1 to 120), // two 45-minute sessions
            sessionMinutes = 45
        )
        assertEquals(65, WeekReviewer.adjustLoad(settings, LoadSuggestion.LIGHTER).dailyMinutes.getValue(1))
        assertEquals(185, WeekReviewer.adjustLoad(settings, LoadSuggestion.FULLER).dailyMinutes.getValue(1))
        assertEquals(settings, WeekReviewer.adjustLoad(settings, LoadSuggestion.NONE))
    }
}