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
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.SubjectRepository

/** Streams without groups: an A/L student studies 3 subjects, so at most 3 can be chosen. */
const val MAX_SUBJECTS = 3

/**
 * One group of a stream (Group 1, Group 2 ...).
 * One subject alone = compulsory. Two or more = the student chooses ONE of them.
 */
data class SubjectGroup(val number: Int, val subjects: List<Subject>) {
    val isCompulsory: Boolean get() = subjects.size == 1
}

data class SubjectSelectUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val stream: StudyStream? = null,
    val selectedIds: Set<String> = emptySet()
) {
    // Group 1, 2, 3 ... (an empty group is skipped but the others keep their number)
    val groups: List<SubjectGroup>
        get() = stream?.groups.orEmpty()
            .mapIndexed { index, subjects -> SubjectGroup(index + 1, subjects) }
            .filter { it.subjects.isNotEmpty() }

    val hasGroups: Boolean get() = groups.isNotEmpty()

    // With groups: one subject per group. Without groups: up to 3 subjects.
    val maxSubjects: Int get() = if (hasGroups) groups.size else MAX_SUBJECTS

    // Without groups: when 3 are ticked the others are locked until one is unticked
    val isFull: Boolean get() = !hasGroups && selectedIds.size >= MAX_SUBJECTS

    val canContinue: Boolean
        get() = if (hasGroups) groups.all { group -> group.subjects.any { it.id in selectedIds } }
        else selectedIds.isNotEmpty()
}

/** Loads the subjects of one stream and keeps the (temporary) choice. */
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
                val earlier = if (previous.streamId == stream.id) previous.subjectIds else emptySet()
                val draft = SubjectSelectUiState(stream = stream)

                val preselected: Set<String> = if (draft.hasGroups) {
                    // Compulsory subjects are always ticked, the others come back as before
                    draft.groups.mapNotNull { group ->
                        if (group.isCompulsory) group.subjects.first().id
                        else group.subjects.firstOrNull { it.id in earlier }?.id
                    }.toSet()
                } else {
                    val validIds = stream.subjects.map { it.id }.toSet()
                    (earlier intersect validIds).take(MAX_SUBJECTS).toSet()
                }

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

    fun toggle(subjectId: String) = _uiState.update { state ->
        val current = state.selectedIds
        val next = if (state.hasGroups) {
            val group = state.groups.firstOrNull { g -> g.subjects.any { it.id == subjectId } }
            when {
                group == null -> current
                group.isCompulsory -> current                       // compulsory: cannot be changed
                subjectId in current -> current - subjectId         // untick
                else -> (current - group.subjects.map { it.id }.toSet()) + subjectId  // one per group
            }
        } else {
            when {
                subjectId in current -> current - subjectId
                current.size < MAX_SUBJECTS -> current + subjectId
                else -> current
            }
        }
        state.copy(selectedIds = next)
    }

    /** Saves the choice in memory only. Returns false if the choice is not complete yet. */
    fun confirm(): Boolean {
        val state = _uiState.value
        val stream = state.stream ?: return false
        if (!state.canContinue) return false

        // Group 1, 2, 3 order when the stream has groups
        val chosen = if (state.hasGroups) {
            state.groups.mapNotNull { group -> group.subjects.firstOrNull { it.id in state.selectedIds } }
        } else {
            stream.subjects.filter { it.id in state.selectedIds }
        }
        if (chosen.isEmpty()) return false
        PaperBrowseSelection.select(stream, chosen)
        return true
    }
}