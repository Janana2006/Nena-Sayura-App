package lk.nenasayura.app.ui.student.papers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.repository.SubjectRepository

/** An A/L student studies 3 subjects, so at most 3 can be chosen. */
const val MAX_SUBJECTS = 3

data class SubjectSelectUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val stream: StudyStream? = null,
    val selectedIds: Set<String> = emptySet()
) {
    val maxSubjects: Int get() = MAX_SUBJECTS

    // true when 3 subjects are already ticked: the other subjects are locked until one is unticked
    val isFull: Boolean get() = selectedIds.size >= MAX_SUBJECTS
}

/** Loads the subjects of one stream and keeps the (temporary) choice, up to 3 subjects. */
class SubjectSelectViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // Navigation argument
    private val streamId: String = savedStateHandle.get<String>("streamId") ?: ""

    private val repository = SubjectRepository()

    private val _uiState = MutableStateFlow(SubjectSelectUiState())
    val uiState: StateFlow<SubjectSelectUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val stream = repository.getStream(streamId)
                if (stream == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "This stream could not be found.")
                    }
                    return@launch
                }
                // If the user comes back to the same stream, keep the earlier ticks
                val previous = PaperBrowseSelection.state.value
                val validIds = stream.subjects.map { it.id }.toSet()
                val preselected =
                    if (previous.streamId == stream.id) (previous.subjectIds intersect validIds).take(MAX_SUBJECTS).toSet()
                    else emptySet()
                _uiState.update {
                    it.copy(isLoading = false, stream = stream, selectedIds = preselected)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load subjects. Check your internet connection and try again."
                    )
                }
            }
        }
    }

    // Untick always works. Tick only works while fewer than 3 subjects are chosen.
    fun toggle(subjectId: String) = _uiState.update {
        val next = when {
            subjectId in it.selectedIds -> it.selectedIds - subjectId
            it.selectedIds.size < MAX_SUBJECTS -> it.selectedIds + subjectId
            else -> it.selectedIds
        }
        it.copy(selectedIds = next)
    }

    /** Saves the choice in memory only. Returns false if nothing is selected. */
    fun confirm(): Boolean {
        val state = _uiState.value
        val stream = state.stream ?: return false
        val chosen = stream.subjects.filter { it.id in state.selectedIds }
        if (chosen.isEmpty()) return false
        PaperBrowseSelection.select(stream, chosen)
        return true
    }
}