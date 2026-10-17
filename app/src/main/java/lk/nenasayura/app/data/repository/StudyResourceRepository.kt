package lk.nenasayura.app.data.repository



import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import lk.nenasayura.app.core.planner.ResourcePool
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.TeacherContent
import java.time.LocalDate

/** Gathers what a student may study, for the timetable generator. */
class StudyResourceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val paperRepository: PaperRepository = PaperRepository(),
    private val markingSchemeRepository: MarkingSchemeRepository = MarkingSchemeRepository(),
    private val guideBookRepository: GuideBookRepository = GuideBookRepository(),
    private val quizRepository: QuizRepository = QuizRepository(),
    private val planRepository: StudyPlanRepository = StudyPlanRepository()
) {
    /** Papers, schemes, books, quizzes and the teacher content this student may open, for their subjects. */
    suspend fun loadPool(uid: String, subjectIds: Set<String>): ResourcePool = coroutineScope {
        val papers = async { paperRepository.getPapers() }
        val schemes = async { markingSchemeRepository.getMarkingSchemes() }
        val books = async { guideBookRepository.getGuideBooks() }
        val quizzes = async { quizRepository.getQuizzes() }
        val content = async { usableTeacherContent(uid, subjectIds) }

        ResourcePool(
            papers = papers.await().filter { it.subjectId in subjectIds },
            markingSchemes = schemes.await().filter { it.subjectId in subjectIds },
            guideBooks = books.await().filter { it.subjectId in subjectIds },
            quizzes = quizzes.await().filter { it.subjectId in subjectIds },
            teacherContent = content.await()
        )
    }

    /** Resources the student already finished in earlier plans, as "type:id". */
    suspend fun getCompletedKeys(uid: String): Set<String> = try {
        planRepository.getSessions(uid, "2000-01-01", LocalDate.now().toString())
            .filter { it.status == SessionStatus.DONE && it.resourceId.isNotBlank() }
            .map { "${it.resourceType}:${it.resourceId}" }
            .toSet()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptySet() // the plan still works, it just may repeat a resource
    }

    // A teacher item is usable when it is marked Free, or its teacher is free,
    // or this student has paid that teacher.
    private suspend fun usableTeacherContent(uid: String, subjectIds: Set<String>): List<TeacherContent> {
        if (subjectIds.isEmpty()) return emptyList()
        return try {
            // "whereIn" accepts at most 10 values at a time
            val all = subjectIds.chunked(10).flatMap { chunk ->
                db.collection("teacher_content")
                    .whereIn("subjectId", chunk)
                    .get()
                    .await()
                    .documents
                    .map { it.toContent() }
            }
            if (all.isEmpty()) return emptyList()

            val paidTeachers = paidTeacherIds(uid)
            val lockedTeachers = all
                .filter { it.access == AccessType.PAID && it.teacherId !in paidTeachers }
                .map { it.teacherId }
                .distinct()
            val freeTeachers = lockedTeachers.filter { isFreeTeacher(it) }.toSet()

            all.filter {
                it.openUrl.isNotBlank() &&
                        (it.access == AccessType.FREE || it.teacherId in paidTeachers || it.teacherId in freeTeachers)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList() // the other resources are still used
        }
    }

    private suspend fun paidTeacherIds(uid: String): Set<String> = try {
        db.collection("payments")
            .whereEqualTo("studentId", uid)
            .get()
            .await()
            .documents
            .filter {
                val status = it.getString("status") ?: "paid"
                status.equals("paid", ignoreCase = true) || status.equals("success", ignoreCase = true)
            }
            .mapNotNull { it.getString("teacherId") }
            .toSet()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptySet()
    }

    // A teacher with no "isFree" field counts as free (the same default the Teacher module uses)
    private suspend fun isFreeTeacher(teacherId: String): Boolean = try {
        db.collection("users").document(teacherId).get().await().getBoolean("isFree") ?: true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    private fun DocumentSnapshot.toContent() = TeacherContent(
        id = id,
        teacherId = getString("teacherId") ?: "",
        teacherName = getString("teacherName") ?: "",
        title = getString("title") ?: "",
        subjectId = getString("subjectId") ?: "",
        subjectName = getString("subjectName") ?: "",
        type = ContentType.fromKey(getString("type")),
        access = AccessType.fromKey(getString("access")),
        fileUrl = getString("fileUrl") ?: "",
        videoUrl = getString("videoUrl") ?: "",
        createdAtMillis = (get("createdAt") as? Timestamp)?.toDate()?.time ?: 0L
    )
}