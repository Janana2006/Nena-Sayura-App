package lk.nenasayura.app.core.browse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject

/** The stream + subjects a student picked before opening Papers. */
data class PaperBrowseState(
    val streamId: String = "",
    val streamName: String = "",
    val subjects: List<Subject> = emptyList()
) {
    val hasSelection: Boolean get() = streamId.isNotEmpty() && subjects.isNotEmpty()
    val subjectIds: Set<String> get() = subjects.map { it.id }.toSet()
}

/**
 * TEMPORARY selection. It lives only in memory (no SharedPreferences / DataStore / Firestore),
 * so when the app process is closed and opened again the student has to choose the stream again.
 */
object PaperBrowseSelection {
    private val _state = MutableStateFlow(PaperBrowseState())
    val state: StateFlow<PaperBrowseState> = _state.asStateFlow()

    fun select(stream: StudyStream, subjects: List<Subject>) {
        _state.update {
            PaperBrowseState(streamId = stream.id, streamName = stream.name, subjects = subjects)
        }
    }

    fun clear() {
        _state.update { PaperBrowseState() }
    }
}