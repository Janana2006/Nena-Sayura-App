package lk.nenasayura.app.ui.guest.search



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.MarkingScheme
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Quiz
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.GuideBookRepository
import lk.nenasayura.app.data.repository.MarkingSchemeRepository
import lk.nenasayura.app.data.repository.PaperRepository
import lk.nenasayura.app.data.repository.QuizRepository
import lk.nenasayura.app.data.repository.SubjectRepository

/** Everything the Search screen needs. All filtering happens here, on the device. */
data class SearchUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val subjects: List<Subject> = emptyList(),
    val papers: List<Paper> = emptyList(),
    val markingSchemes: List<MarkingScheme> = emptyList(),
    val guideBooks: List<GuideBook> = emptyList(),
    val quizzes: List<Quiz> = emptyList(),
    val query: String = "",
    val selectedStream: String? = null,
    val selectedSubjectId: String? = null
) {
    fun subjectName(subjectId: String): String =
        subjects.firstOrNull { it.id == subjectId }?.name ?: ""

    /** Streams come from the data, e.g. "Physical Science", "Commerce". */
    val streams: List<String>
        get() = subjects.map { it.stream }.filter { it.isNotBlank() }.distinct().sorted()

    val selectedSubject: Subject?
        get() = subjects.firstOrNull { it.id == selectedSubjectId }

    /** Resources are shown only after the student types something or picks a subject. */
    val hasCriteria: Boolean
        get() = query.isNotBlank() || selectedSubjectId != null

    val subjectResults: List<Subject>
        get() {
            val text = query.trim()
            return subjects.filter { subject ->
                (selectedStream == null || subject.stream == selectedStream) &&
                        (text.isEmpty() ||
                                subject.name.contains(text, ignoreCase = true) ||
                                subject.stream.contains(text, ignoreCase = true))
            }
        }

    val paperResults: List<Paper>
        get() = papers.filter { matches(it.title, it.subjectId) }

    val markingSchemeResults: List<MarkingScheme>
        get() = markingSchemes.filter { matches(it.title, it.subjectId) }

    val guideBookResults: List<GuideBook>
        get() = guideBooks.filter { matches(it.title, it.subjectId) }

    val quizResults: List<Quiz>
        get() = quizzes.filter { matches(it.title, it.subjectId) }

    val resourceCount: Int
        get() = paperResults.size + markingSchemeResults.size +
                guideBookResults.size + quizResults.size

    // A resource matches when its subject passes the stream/subject filters
    // and the text appears in its title, its subject name or the subject's stream.
    private fun matches(title: String, subjectId: String): Boolean {
        val subject = subjects.firstOrNull { it.id == subjectId }
        if (selectedSubjectId != null && subjectId != selectedSubjectId) return false
        if (selectedStream != null && subject?.stream != selectedStream) return false
        val text = query.trim()
        return text.isEmpty() ||
                title.contains(text, ignoreCase = true) ||
                subject?.name?.contains(text, ignoreCase = true) == true ||
                subject?.stream?.contains(text, ignoreCase = true) == true
    }
}

class SearchViewModel(
    private val subjectRepository: SubjectRepository = SubjectRepository(),
    private val paperRepository: PaperRepository = PaperRepository(),
    private val markingSchemeRepository: MarkingSchemeRepository = MarkingSchemeRepository(),
    private val guideBookRepository: GuideBookRepository = GuideBookRepository(),
    private val quizRepository: QuizRepository = QuizRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load all five collections at the same time
                coroutineScope {
                    val subjectsJob = async { subjectRepository.getSubjects() }
                    val papersJob = async { paperRepository.getPapers() }
                    val schemesJob = async { markingSchemeRepository.getMarkingSchemes() }
                    val booksJob = async { guideBookRepository.getGuideBooks() }
                    val quizzesJob = async { quizRepository.getQuizzes() }

                    val subjects = subjectsJob.await()
                    val papers = papersJob.await()
                    val schemes = schemesJob.await()
                    val books = booksJob.await()
                    val quizzes = quizzesJob.await()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            subjects = subjects,
                            papers = papers,
                            markingSchemes = schemes,
                            guideBooks = books,
                            quizzes = quizzes
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load search data. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    // Changing the stream also clears a chosen subject, which might belong to another stream
    fun onStreamSelected(stream: String?) = _uiState.update {
        it.copy(selectedStream = stream, selectedSubjectId = null)
    }

    // Picking a subject shows everything for that subject, so the typed text is cleared
    fun onSubjectSelected(subjectId: String?) = _uiState.update {
        if (subjectId == null) it.copy(selectedSubjectId = null)
        else it.copy(selectedSubjectId = subjectId, query = "")
    }
}