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
import lk.nenasayura.app.core.browse.PaperBrowseSelection

/** Everything the Papers screen needs to draw itself. */
data class PapersUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val papers: List<Paper> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val selectedType: String? = null,
    val selectedYear: Int? = null,
    val selectedProvince: String? = null,   // Provincial papers only
    val selectedTerm: Int? = null,          // Provincial papers only (1-3)
    val query: String = ""
) {
    fun subjectName(subjectId: String): String =
        subjects.firstOrNull { it.id == subjectId }?.name ?: ""

    // Only offer filters that actually exist in the data
    val availableTypes: List<String>
        get() = papers.map { it.type }.filter { it.isNotBlank() }.distinct().sorted()

    // Only the years that the selected subject has papers for
    val availableYears: List<Int>
        get() = papers
            .filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }
            .map { it.year }.filter { it > 0 }.distinct().sortedDescending()

    // Only the provinces that the selected subject has papers for
    val availableProvinces: List<String>
        get() = papers
            .filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }
            .map { it.province }.filter { it.isNotBlank() }.distinct().sorted()

    // Only the terms (1-3) that the selected subject has papers for
    val availableTerms: List<Int>
        get() = papers
            .filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }
            .map { it.term }.filter { it in 1..3 }.distinct().sorted()

    val availableSubjects: List<Subject>
        get() {
            val usedIds = papers.map { it.subjectId }.toSet()
            return subjects.filter { it.id in usedIds }
        }

    // The subject is always selected (there is no "All" chip), so it does not count as a filter
    val hasActiveFilters: Boolean
        get() = selectedType != null || selectedYear != null ||
                selectedProvince != null || selectedTerm != null || query.isNotBlank()

    val filteredPapers: List<Paper>
        get() {
            val text = query.trim()
            return papers.filter { paper ->
                (selectedSubjectId == null || paper.subjectId == selectedSubjectId) &&
                        (selectedType == null || paper.type == selectedType) &&
                        (selectedYear == null || paper.year == selectedYear) &&
                        (selectedProvince == null || paper.province == selectedProvince) &&
                        (selectedTerm == null || paper.term == selectedTerm) &&
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
                // Students pick stream + subjects first. null = no restriction (guests).
                val browse = PaperBrowseSelection.state.value
                val allowedIds = if (browse.hasSelection) browse.subjectIds else null

                // Keep only the papers of the chosen category (and chosen subjects)
                val categoryPapers = papers.filter { paper ->
                    paper.isInCategory(category) &&
                            (allowedIds == null || paper.subjectId in allowedIds)
                }
                val visibleSubjects = if (allowedIds == null) subjects
                else subjects.filter { s -> s.id in allowedIds }

                // One subject is always selected: keep the current one, otherwise the first one with papers
                val usedIds = categoryPapers.map { paper -> paper.subjectId }.toSet()
                val firstSubjectId = visibleSubjects.firstOrNull { s -> s.id in usedIds }?.id

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        papers = categoryPapers,
                        subjects = visibleSubjects,
                        selectedSubjectId = it.selectedSubjectId
                            ?.takeIf { id -> id in usedIds } ?: firstSubjectId
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
    // Changing the subject also clears the year, province and term, because they differ for each subject
    fun onSubjectSelected(subjectId: String?) =
        _uiState.update {
            it.copy(
                selectedSubjectId = subjectId,
                selectedYear = null,
                selectedProvince = null,
                selectedTerm = null
            )
        }
    fun onTypeSelected(type: String?) = _uiState.update { it.copy(selectedType = type) }
    fun onYearSelected(year: Int?) = _uiState.update { it.copy(selectedYear = year) }
    fun onProvinceSelected(province: String?) = _uiState.update { it.copy(selectedProvince = province) }
    fun onTermSelected(term: Int?) = _uiState.update { it.copy(selectedTerm = term) }

    // Clears type, year, province, term and search. The selected subject stays.
    fun clearFilters() = _uiState.update {
        it.copy(
            selectedType = null,
            selectedYear = null,
            selectedProvince = null,
            selectedTerm = null,
            query = ""
        )
    }
}