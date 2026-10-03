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

    // Only offer filters that actually exist in the data
    val availableYears: List<Int>
        get() = schemes.map { it.year }.filter { it > 0 }.distinct().sortedDescending()

    val availableSubjects: List<Subject>
        get() {
            val usedIds = schemes.map { it.subjectId }.toSet()
            return subjects.filter { it.id in usedIds }
        }

    val hasActiveFilters: Boolean
        get() = selectedSubjectId != null || selectedYear != null || query.isNotBlank()

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

class MarkingSchemesViewModel(
    private val markingSchemeRepository: MarkingSchemeRepository = MarkingSchemeRepository(),
    private val subjectRepository: SubjectRepository = SubjectRepository()
) : ViewModel() {

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
                _uiState.update {
                    it.copy(isLoading = false, schemes = schemes, subjects = subjects)
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
    fun onSubjectSelected(subjectId: String?) = _uiState.update { it.copy(selectedSubjectId = subjectId) }
    fun onYearSelected(year: Int?) = _uiState.update { it.copy(selectedYear = year) }

    fun clearFilters() = _uiState.update {
        it.copy(selectedSubjectId = null, selectedYear = null, query = "")
    }
}