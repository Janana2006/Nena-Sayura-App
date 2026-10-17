package lk.nenasayura.app.core.planner



import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.MarkingScheme
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.ResourceTypes
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.data.model.TeacherContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import lk.nenasayura.app.data.model.SessionStatus

class ResourceAttacherTest {

    private fun session(kind: SessionKind, subject: String = "chem") = StudySession(
        date = "2026-10-20", startMinute = 1110, subjectId = subject, subjectName = subject, kind = kind
    )

    private fun video(id: String, subject: String = "chem") = TeacherContent(
        id = id, teacherId = "t1", title = "Video $id", subjectId = subject,
        type = ContentType.VIDEO_LINK, videoUrl = "https://video/$id"
    )

    private fun note(id: String) = TeacherContent(
        id = id, teacherId = "t1", title = "Note $id", subjectId = "chem",
        type = ContentType.SHORT_NOTE, fileUrl = "https://file/$id"
    )

    private fun paper(id: String, year: Int) = Paper(
        id = id, title = "Paper $id", subjectId = "chem", year = year, type = "past", fileUrl = "https://paper/$id"
    )

    private fun scheme(id: String, year: Int) = MarkingScheme(
        id = id, title = "Scheme $id", subjectId = "chem", year = year, type = "past", fileUrl = "https://scheme/$id"
    )

    private fun quiz(id: String) = Quiz(id = id, title = "Quiz $id", subjectId = "chem")

    @Test
    fun aVideoSessionGetsATeacherVideo() {
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.LEARN_VIDEO)),
            ResourcePool(teacherContent = listOf(video("v1")))
        ).single()
        assertEquals(ResourceTypes.TEACHER_VIDEO, result.resourceType)
        assertEquals("v1", result.resourceId)
        assertEquals("https://video/v1", result.resourceUrl)
        assertEquals(SessionKind.LEARN_VIDEO, result.kind)
    }

    @Test
    fun aVideoSessionFallsBackToANoteAndTheKindFollowsTheResource() {
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.LEARN_VIDEO)),
            ResourcePool(teacherContent = listOf(note("n1")))
        ).single()
        assertEquals(ResourceTypes.TEACHER_FILE, result.resourceType)
        assertEquals(SessionKind.LEARN_NOTE, result.kind)
    }

    @Test
    fun aResourceIsNotRepeatedWhileOthersAreUnused() {
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.LEARN_VIDEO), session(SessionKind.LEARN_VIDEO)),
            ResourcePool(teacherContent = listOf(video("v1"), video("v2")))
        )
        assertEquals(setOf("v1", "v2"), result.map { it.resourceId }.toSet())
    }

    @Test
    fun completedResourcesAreSkipped() {
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.LEARN_VIDEO)),
            ResourcePool(teacherContent = listOf(video("v1"), video("v2"))),
            completedKeys = setOf("${ResourceTypes.TEACHER_VIDEO}:v1")
        ).single()
        assertEquals("v2", result.resourceId)
    }

    @Test
    fun aMarkingReviewGetsTheSchemeOfThePaperJustPlanned() {
        val pool = ResourcePool(
            papers = listOf(paper("p2022", 2022), paper("p2023", 2023)),
            markingSchemes = listOf(scheme("s2022", 2022), scheme("s2023", 2023))
        )
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.PAPER), session(SessionKind.MARKING_REVIEW)),
            pool
        )
        assertEquals("p2023", result[0].resourceId)      // newest paper first
        assertEquals("s2023", result[1].resourceId)      // its own scheme, not the first one in the list
    }

    @Test
    fun aRevisionSessionReusesEarlierLearningMaterial() {
        val pool = ResourcePool(
            teacherContent = listOf(note("n1")),
            quizzes = listOf(quiz("q1"))
        )
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.LEARN_NOTE), session(SessionKind.REVISION)),
            pool
        )
        assertEquals("n1", result[1].resourceId)
        assertEquals(SessionKind.REVISION, result[1].kind)
    }

    @Test
    fun aQuizIsReusedWhenThereAreMoreSessionsThanQuizzes() {
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.QUIZ), session(SessionKind.QUIZ)),
            ResourcePool(quizzes = listOf(quiz("q1")))
        )
        assertTrue(result.all { it.resourceId == "q1" })
    }

    @Test
    fun aSessionWithNoResourcesStaysAsItIs() {
        val original = session(SessionKind.PAPER)
        assertEquals(listOf(original), ResourceAttacher.attach(listOf(original), ResourcePool()))
    }

    @Test
    fun onlyTheSessionsOwnSubjectIsUsed() {
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.LEARN_VIDEO, subject = "chem")),
            ResourcePool(teacherContent = listOf(video("v1", subject = "phys")))
        ).single()
        assertEquals("", result.resourceId)
    }

    @Test
    fun aMissedSessionIsCarriedIntoTheFirstSessionOfThatSubject() {
        val missed = StudySession(
            date = "2026-10-12", startMinute = 1110, subjectId = "chem", subjectName = "chem",
            kind = SessionKind.LEARN_VIDEO, resourceType = ResourceTypes.TEACHER_VIDEO,
            resourceId = "v1", resourceTitle = "Video v1", resourceUrl = "https://video/v1",
            status = SessionStatus.MISSED
        )
        val result = ResourceAttacher.attach(
            listOf(session(SessionKind.QUIZ), session(SessionKind.LEARN_VIDEO)),
            ResourcePool(quizzes = listOf(quiz("q1")), teacherContent = listOf(video("v1"), video("v2"))),
            carryOver = listOf(missed)
        )
        assertEquals("v1", result[0].resourceId)
        assertEquals("2026-10-12", result[0].originalDate)
        assertEquals(SessionKind.LEARN_VIDEO, result[0].kind)
        assertEquals("v2", result[1].resourceId) // v1 is already taken, so the next video is used
    }
}