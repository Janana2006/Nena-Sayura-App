package lk.nenasayura.app.ui.guest.markingschemes


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
import lk.nenasayura.app.data.model.MarkingScheme
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.MarkingSchemeRepository
import lk.nenasayura.app.data.repository.SubjectRepository
import androidx.lifecycle.SavedStateHandle
import lk.nenasayura.app.data.model.isInCategory
import lk.nenasayura.app.core.browse.PaperBrowseSelection

/** Everything the Marking Schemes screen needs to draw itself. */
data class MarkingSchemesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val schemes: List<MarkingScheme> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val selectedSubjectId: String? = null,
    val selectedYear: Int? = null,
    val query: String = ""
) {
    fun subjectName(subjectId: String): String =
        subjects.firstOrNull { it.id == subjectId }?.name ?: ""

    // Only the years that the selected subject has marking schemes for
    val availableYears: List<Int>
        get() = schemes
            .filter { selectedSubjectId == null || it.subjectId == selectedSubjectId }
            .map { it.year }.filter { it > 0 }.distinct().sortedDescending()

    val availableSubjects: List<Subject>
        get() {
            val usedIds = schemes.map { it.subjectId }.toSet()
            return subjects.filter { it.id in usedIds }
        }

    // The subject is always selected (there is no "All" chip), so it does not count as a filter
    val hasActiveFilters: Boolean
        get() = selectedYear != null || query.isNotBlank()

    val filteredSchemes: List<MarkingScheme>
        get() {
            val text = query.trim()
            return schemes.filter { scheme ->
                (selectedSubjectId == null || scheme.subjectId == selectedSubjectId) &&
                        (selectedYear == null || scheme.year == selectedYear) &&
                        (text.isEmpty() ||
                                scheme.title.contains(text, ignoreCase = true) ||
                                subjectName(scheme.subjectId).contains(text, ignoreCase = true))
            }
        }
}

class MarkingSchemesViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // "past", "model" or "provincial" (the navigation argument)
    val category: String = savedStateHandle.get<String>("category") ?: "past"

    private val markingSchemeRepository = MarkingSchemeRepository()
    private val subjectRepository = SubjectRepository()

    private val _uiState = MutableStateFlow(MarkingSchemesUiState())
    val uiState: StateFlow<MarkingSchemesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load marking schemes and subjects at the same time
                val (schemes, subjects) = coroutineScope {
                    val schemesJob = async { markingSchemeRepository.getMarkingSchemes() }
                    val subjectsJob = async { subjectRepository.getSubjects() }
                    schemesJob.await() to subjectsJob.await()
                }
                // Students pick stream + subjects first. null = no restriction (guests).
                val browse = PaperBrowseSelection.state.value
                val allowedIds = if (browse.hasSelection) browse.subjectIds else null

                // Keep only the schemes of the chosen category (and chosen subjects)
                val categorySchemes = schemes.filter { scheme ->
                    scheme.isInCategory(category) &&
                            (allowedIds == null || scheme.subjectId in allowedIds)
                }
                val visibleSubjects = if (allowedIds == null) subjects
                else subjects.filter { s -> s.id in allowedIds }

                // One subject is always selected: keep the current one, otherwise the first one with schemes
                val usedIds = categorySchemes.map { scheme -> scheme.subjectId }.toSet()
                val firstSubjectId = visibleSubjects.firstOrNull { s -> s.id in usedIds }?.id

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        schemes = categorySchemes,
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
                        errorMessage = "Could not load marking schemes. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
    // Changing the subject also clears the year, because the years differ for each subject
    fun onSubjectSelected(subjectId: String?) =
        _uiState.update { it.copy(selectedSubjectId = subjectId, selectedYear = null) }
    fun onYearSelected(year: Int?) = _uiState.update { it.copy(selectedYear = year) }

    // Clears year and search. The selected subject stays.
    fun clearFilters() = _uiState.update {
        it.copy(selectedYear = null, query = "")
    }
}