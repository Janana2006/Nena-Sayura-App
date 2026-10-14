package lk.nenasayura.app.ui.guest.guidebooks

import androidx.lifecycle.SavedStateHandle
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
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.isInCategory
import lk.nenasayura.app.data.repository.GuideBookRepository
import lk.nenasayura.app.data.repository.SubjectRepository

/** Everything the Guide Books screen needs to draw itself. */
data class GuideBooksUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val books: List<GuideBook> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val query: String = "",
    // Set when a student opened the books of one subject (stream -> category -> subject)
    val fixedSubjectId: String? = null
) {
    fun subjectName(subjectId: String): String =
        subjects.firstOrNull { it.id == subjectId }?.name ?: ""

    // Only offer subjects that actually have guide books
    val availableSubjects: List<Subject>
        get() {
            val usedIds = books.map { it.subjectId }.toSet()
            return subjects.filter { it.id in usedIds }
        }

    val hasActiveFilters: Boolean
        get() = selectedSubjectId != null || query.isNotBlank()

    val filteredBooks: List<GuideBook>
        get() {
            val text = query.trim()
            return books.filter { book ->
                (selectedSubjectId == null || book.subjectId == selectedSubjectId) &&
                        (text.isEmpty() ||
                                book.title.contains(text, ignoreCase = true) ||
                                book.author.contains(text, ignoreCase = true) ||
                                subjectName(book.subjectId).contains(text, ignoreCase = true))
            }
        }
}

class GuideBooksViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // "guide" or "resource" (the navigation argument)
    val category: String = savedStateHandle.get<String>("category") ?: "guide"

    // Optional navigation argument: only the books of this subject
    private val subjectId: String? = savedStateHandle.get<String>("subjectId")

    private val guideBookRepository = GuideBookRepository()
    private val subjectRepository = SubjectRepository()

    private val _uiState = MutableStateFlow(GuideBooksUiState(fixedSubjectId = subjectId))
    val uiState: StateFlow<GuideBooksUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load guide books and subjects at the same time
                val (books, subjects) = coroutineScope {
                    val booksJob = async { guideBookRepository.getGuideBooks() }
                    val subjectsJob = async { subjectRepository.getSubjects() }
                    booksJob.await() to subjectsJob.await()
                }

                // DEBUG: see this in Logcat (filter: GuideDebug). Remove after the problem is found.
                android.util.Log.d(
                    "GuideDebug",
                    "category=$category subjectId=$subjectId total=${books.size} " +
                            "categories=${books.map { it.category }} " +
                            "bookSubjectIds=${books.map { it.subjectId }} " +
                            "inThisCategory=${books.count { it.isInCategory(category) }} " +
                            "inCategoryAndSubject=${books.count { it.isInCategory(category) && (subjectId == null || it.subjectId == subjectId) }}"
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Keep only the books of the chosen category (and the chosen subject)
                        books = books.filter { book ->
                            book.isInCategory(category) &&
                                    (subjectId == null || book.subjectId == subjectId)
                        },
                        subjects = subjects
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load guide books. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onSubjectSelected(subjectId: String?) = _uiState.update { it.copy(selectedSubjectId = subjectId) }

    fun clearFilters() = _uiState.update {
        it.copy(selectedSubjectId = null, query = "")
    }
}