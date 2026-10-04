package lk.nenasayura.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.repository.SetupRepository

data class SetupUiState(
    val role: String = "student",
    val loading: Boolean = true,
    val streams: List<StudyStream> = emptyList(),
    val selectedStreamId: String? = null,
    val selectedSubjectIds: Set<String> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
) {
    val selectedStream: StudyStream?
        get() = streams.find { it.id == selectedStreamId }

    // Teacher: subject 1k, Student: subjects 3k
    val maxSubjects: Int
        get() = if (role == "teacher") 1 else 3

    // Stream eke subjects 3t wada kamai nam, thiyena okkoma select karanna oni
    val requiredCount: Int
        get() = minOf(maxSubjects, selectedStream?.subjects?.size ?: maxSubjects)

    val canConfirm: Boolean
        get() = selectedStream != null &&
                requiredCount > 0 &&
                selectedSubjectIds.size == requiredCount
}

class SetupViewModel(
    private val repo: SetupRepository = SetupRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    // Screen eka open wena hama welawema fresh start ekak
    fun start(role: String = "teacher") {
        _state.value = SetupUiState(role = role)
        loadStreams()
    }

    fun loadStreams() {
        _state.update { it.copy(loading = true, error = null) }
        repo.getStreams { result ->
            result.fold(
                onSuccess = { list -> _state.update { it.copy(loading = false, streams = list) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(loading = false, error = e.localizedMessage ?: "Failed to load streams")
                    }
                }
            )
        }
    }

    fun selectStream(id: String) {
        if (_state.value.selectedStreamId == id) return
        // stream wenas unama subjects clear wenawa
        _state.update { it.copy(selectedStreamId = id, selectedSubjectIds = emptySet()) }
    }

    fun toggleSubject(id: String) {
        _state.update { s ->
            val current = s.selectedSubjectIds
            val next = when {
                s.maxSubjects == 1 -> setOf(id)                  // teacher: ekak, aluth ekak select kalama replace wenawa
                id in current -> current - id                    // untick
                current.size < s.maxSubjects -> current + id     // student: 3t wada select karanna ba
                else -> current
            }
            s.copy(selectedSubjectIds = next)
        }
    }

    fun confirm() {
        val s = _state.value
        val stream = s.selectedStream ?: return
        if (!s.canConfirm) return
        val chosen = stream.subjects.filter { it.id in s.selectedSubjectIds }

        _state.update { it.copy(saving = true, error = null) }
        repo.saveSetup(s.role, stream, chosen) { result ->
            result.fold(
                onSuccess = { _state.update { it.copy(saving = false, done = true) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(saving = false, error = e.localizedMessage ?: "Failed to save")
                    }
                }
            )
        }
    }

    fun consumeDone() {
        _state.update { it.copy(done = false) }
    }
}