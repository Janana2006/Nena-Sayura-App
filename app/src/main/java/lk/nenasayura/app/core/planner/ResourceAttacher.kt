package lk.nenasayura.app.core.planner



import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.MarkingScheme
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.ResourceTypes
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.data.model.TeacherContent

/** Everything the student may open. The repository limits it to their subjects and access rights. */
data class ResourcePool(
    val papers: List<Paper> = emptyList(),
    val markingSchemes: List<MarkingScheme> = emptyList(),
    val guideBooks: List<GuideBook> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val teacherContent: List<TeacherContent> = emptyList()
)

/**
 * Gives each study session a real resource. Pure Kotlin, so it is easy to test.
 *
 * Rules:
 *  - fallback chain when a subject has no resource of the wanted type
 *  - a resource is not planned twice while unused ones exist (done ones count as used)
 *  - a marking review gets the scheme of the paper just planned
 *  - revision reuses material the student learned earlier in the plan
 */
object ResourceAttacher {

    private class Ref(
        val type: String,
        val id: String,
        val title: String,
        val url: String,
        /** The session kind that fits this resource. */
        val kind: SessionKind,
        val paper: Paper? = null,
        val scheme: MarkingScheme? = null
    ) {
        val key: String get() = "$type:$id"
    }

    private class SubjectRefs(
        val videos: List<Ref>,
        val notes: List<Ref>,
        val books: List<Ref>,
        val papers: List<Ref>,
        val schemes: List<Ref>,
        val quizzes: List<Ref>
    )

    /**
     * @param completedKeys resources already finished in earlier plans, as "type:id"
     *                      (for example "quiz:abc"); they are used last
     */
    fun attach(
        sessions: List<StudySession>,
        pool: ResourcePool,
        completedKeys: Set<String> = emptySet()
    ): List<StudySession> {
        val refsBySubject = HashMap<String, SubjectRefs>()
        val used = completedKeys.toMutableSet()          // planned or already done
        val timesPlanned = HashMap<String, Int>()        // for even reuse when everything is used
        val lastPaper = HashMap<String, Paper>()         // subjectId -> latest paper planned
        val learned = HashMap<String, MutableList<Ref>>() // subjectId -> notes/videos/books planned

        return sessions.map { session ->
            if (session.kind == SessionKind.CUSTOM) return@map session

            val refs = refsBySubject.getOrPut(session.subjectId) { buildRefs(pool, session.subjectId) }
            val learnedRefs = learned[session.subjectId].orEmpty()

            val ref = if (session.kind == SessionKind.REVISION && learnedRefs.isNotEmpty()) {
                // Revise what was learned earlier, the least revised item first
                leastPlanned(listOf(learnedRefs), timesPlanned)
            } else {
                val chain = chainFor(session.kind, refs, lastPaper[session.subjectId])
                firstUnused(chain, used) ?: leastPlanned(chain, timesPlanned)
            } ?: return@map session

            used.add(ref.key)
            timesPlanned[ref.key] = (timesPlanned[ref.key] ?: 0) + 1
            ref.paper?.let { lastPaper[session.subjectId] = it }
            if (ref.kind == SessionKind.LEARN_VIDEO || ref.kind == SessionKind.LEARN_NOTE || ref.kind == SessionKind.LEARN_BOOK) {
                learned.getOrPut(session.subjectId) { mutableListOf() }.add(ref)
            }

            session.copy(
                // The type follows the resource, so a "Video lesson" never shows a book
                kind = if (session.kind == SessionKind.REVISION) SessionKind.REVISION else ref.kind,
                resourceType = ref.type,
                resourceId = ref.id,
                resourceTitle = ref.title,
                resourceUrl = ref.url
            )
        }
    }

    // ---------- which resources suit which session ----------

    private fun chainFor(kind: SessionKind, refs: SubjectRefs, lastPaper: Paper?): List<List<Ref>> = when (kind) {
        SessionKind.LEARN_VIDEO -> listOf(refs.videos, refs.notes, refs.books, refs.quizzes)
        SessionKind.LEARN_NOTE -> listOf(refs.notes, refs.books, refs.videos, refs.quizzes)
        SessionKind.LEARN_BOOK -> listOf(refs.books, refs.notes, refs.videos, refs.quizzes)
        SessionKind.PAPER -> listOf(refs.papers, refs.quizzes)
        SessionKind.MARKING_REVIEW -> listOf(matchingSchemes(refs.schemes, lastPaper), refs.schemes)
        SessionKind.QUIZ -> listOf(refs.quizzes)
        // Revision with nothing learned yet: quizzes first, then any material
        SessionKind.REVISION -> listOf(refs.quizzes, refs.notes, refs.books, refs.videos)
        SessionKind.CUSTOM -> emptyList()
    }

    /** The schemes that belong to the paper just planned (same year and same type). */
    private fun matchingSchemes(schemes: List<Ref>, paper: Paper?): List<Ref> {
        if (paper == null) return emptyList()
        return schemes.filter {
            val scheme = it.scheme
            scheme != null && scheme.year == paper.year &&
                    normalizedType(scheme.type) == normalizedType(paper.type)
        }
    }

    private fun normalizedType(type: String): String = type.trim().lowercase().ifEmpty { "past" }

    private fun firstUnused(chain: List<List<Ref>>, used: Set<String>): Ref? {
        for (list in chain) {
            list.firstOrNull { it.key !in used }?.let { return it }
        }
        return null
    }

    // Everything has been used already: take the least used item of the first non-empty list
    private fun leastPlanned(chain: List<List<Ref>>, timesPlanned: Map<String, Int>): Ref? {
        val list = chain.firstOrNull { it.isNotEmpty() } ?: return null
        return list.minByOrNull { timesPlanned[it.key] ?: 0 }
    }

    // ---------- the student's resources for one subject ----------

    private fun buildRefs(pool: ResourcePool, subjectId: String): SubjectRefs {
        val content = pool.teacherContent.filter { it.subjectId == subjectId }

        val videos = content
            .filter { it.type == ContentType.VIDEO_LINK && it.videoUrl.isNotBlank() }
            .sortedByDescending { it.createdAtMillis }
            .map { Ref(ResourceTypes.TEACHER_VIDEO, it.id, it.title, it.videoUrl, SessionKind.LEARN_VIDEO) }

        val notes = content
            .filter { it.type in NOTE_TYPES && it.fileUrl.isNotBlank() }
            .sortedWith(compareBy<TeacherContent> { NOTE_TYPES.indexOf(it.type) }.thenByDescending { it.createdAtMillis })
            .map { Ref(ResourceTypes.TEACHER_FILE, it.id, it.title, it.fileUrl, SessionKind.LEARN_NOTE) }

        // Resource books first, then guide books
        val books = pool.guideBooks
            .filter { it.subjectId == subjectId && it.fileUrl.isNotBlank() }
            .sortedWith(compareBy<GuideBook> { if (it.category.trim().lowercase() == "resource") 0 else 1 }.thenBy { it.title })
            .map { Ref(ResourceTypes.GUIDE_BOOK, it.id, it.title, it.fileUrl, SessionKind.LEARN_BOOK) }

        // Past papers first (newest year first), then model, then provincial; teachers' model papers last
        val papers = pool.papers
            .filter { it.subjectId == subjectId && it.fileUrl.isNotBlank() }
            .sortedWith(compareBy<Paper> { paperRank(it.type) }.thenByDescending { it.year }.thenBy { it.title })
            .map { Ref(ResourceTypes.PAPER, it.id, it.title, it.fileUrl, SessionKind.PAPER, paper = it) } +
                content
                    .filter { it.type == ContentType.MODEL_PAPER && it.fileUrl.isNotBlank() }
                    .sortedByDescending { it.createdAtMillis }
                    .map { Ref(ResourceTypes.TEACHER_FILE, it.id, it.title, it.fileUrl, SessionKind.PAPER) }

        val schemes = pool.markingSchemes
            .filter { it.subjectId == subjectId && it.fileUrl.isNotBlank() }
            .sortedWith(compareByDescending<MarkingScheme> { it.year }.thenBy { it.title })
            .map { Ref(ResourceTypes.MARKING_SCHEME, it.id, it.title, it.fileUrl, SessionKind.MARKING_REVIEW, scheme = it) }

        val quizzes = pool.quizzes
            .filter { it.subjectId == subjectId }
            .sortedBy { it.title }
            .map { Ref(ResourceTypes.QUIZ, it.id, it.title, "", SessionKind.QUIZ) }

        return SubjectRefs(videos, notes, books, papers, schemes, quizzes)
    }

    private val NOTE_TYPES = listOf(ContentType.SHORT_NOTE, ContentType.NOTE, ContentType.TUTE)

    private fun paperRank(type: String): Int = when (normalizedType(type)) {
        "past" -> 0
        "model" -> 1
        "provincial" -> 2
        else -> 3
    }
}