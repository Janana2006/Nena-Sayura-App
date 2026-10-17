package lk.nenasayura.app.core.browse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject

/** The stream a student picked before opening Guide Books (with its subjects). */
data class GuideBrowseState(
    val streamId: String = "",
    val streamName: String = "",
    val subjects: List<Subject> = emptyList()
) {
    val hasSelection: Boolean get() = streamId.isNotEmpty()
}

/**
 * TEMPORARY selection. It lives only in memory (no SharedPreferences / DataStore / Firestore),
 * so when the app is closed and opened again the student has to choose the stream again.
 */
object GuideBrowseSelection {
    private val _state = MutableStateFlow(GuideBrowseState())
    val state: StateFlow<GuideBrowseState> = _state.asStateFlow()

    fun select(stream: StudyStream) {
        _state.update {
            GuideBrowseState(
                streamId = stream.id,
                streamName = stream.name,
                subjects = stream.subjects
            )
        }
    }

    /** Guests: Guide Books uses the same stream + subjects chosen for Papers / Marking Schemes. */
    fun selectFrom(browse: PaperBrowseState) {
        _state.update {
            GuideBrowseState(
                streamId = browse.streamId,
                streamName = browse.streamName,
                subjects = browse.subjects
            )
        }
    }

    fun clear() {
        _state.update { GuideBrowseState() }
    }
}