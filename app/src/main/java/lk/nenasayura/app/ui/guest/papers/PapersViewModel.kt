package lk.nenasayura.app.ui.guest.papers



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
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.PaperRepository
import lk.nenasayura.app.data.repository.SubjectRepository
import androidx.lifecycle.SavedStateHandle
import lk.nenasayura.app.data.model.isInCategory

/** Everything the Papers screen needs to draw itself. */
data class PapersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val papers: List<Paper> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val selectedType: String? = null,
    val selectedYear: Int? = null,
    val query: String = ""
) {
    fun subjectName(subjectId: String): String =
        subjects.firstOrNull { it.id == subjectId }?.name ?: ""

    // Only offer filters that actually exist in the data
    val availableTypes: List<String>
        get() = papers.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()

    val availableYears: List<Int>
        get() = papers.map { it.year }.filter { it > 0 }.distinct().sortedDescending()

    val availableSubjects: List<Subject>
        get() {
            val usedIds = papers.map { it.subjectId }.toSet()
            return subjects.filter { it.id in usedIds }
        }

    val hasActiveFilters: Boolean
        get() = selectedSubjectId != null || selectedType != null ||
                selectedYear != null || query.isNotBlank()

    val filteredPapers: List<Paper>
        get() {
            val text = query.trim()
            return papers.filter { paper ->
                (selectedSubjectId == null || paper.subjectId == selectedSubjectId) &&
                        (selectedType == null || paper.type == selectedType) &&
                        (selectedYear == null || paper.year == selectedYear) &&
                        (text.isEmpty() ||
                                paper.title.contains(text, ignoreCase = true) ||
                                subjectName(paper.subjectId).contains(text, ignoreCase = true))
            }
        }
}

class PapersViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // "past", "model" or "provincial" (the navigation argument)
    val category: String = savedStateHandle.get<String>("category") ?: "past"

    private val paperRepository = PaperRepository()
    private val subjectRepository = SubjectRepository()

    private val _uiState = MutableStateFlow(PapersUiState())
    val uiState: StateFlow<PapersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load papers and subjects at the same time
                val (papers, subjects) = coroutineScope {
                    val papersJob = async { paperRepository.getPapers() }
                    val subjectsJob = async { subjectRepository.getSubjects() }
                    papersJob.await() to subjectsJob.await()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Keep only the papers of the chosen category
                        papers = papers.filter { paper -> paper.isInCategory(category) },
                        subjects = subjects
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load papers. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    fun onSubjectSelected(subjectId: String?) = _uiState.update { it.copy(selectedSubjectId = subjectId) }
    fun onTypeSelected(type: String?) = _uiState.update { it.copy(selectedType = type) }
    fun onYearSelected(year: Int?) = _uiState.update { it.copy(selectedYear = year) }

    fun clearFilters() = _uiState.update {
        it.copy(selectedSubjectId = null, selectedType = null, selectedYear = null, query = "")
    }
}