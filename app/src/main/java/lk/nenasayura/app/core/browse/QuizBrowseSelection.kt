package lk.nenasayura.app.core.browse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lk.nenasayura.app.data.model.StudyStream

/** The stream a student picked before opening Quizzes. */
data class QuizBrowseState(
    val streamId: String = "",
    val streamName: String = ""
) {
    val hasSelection: Boolean get() = streamId.isNotEmpty()
}

/**
 * TEMPORARY selection. It lives only in memory (no SharedPreferences / DataStore / Firestore),
 * so when the app process is closed and opened again the student has to choose the stream again.
 */
object QuizBrowseSelection {
    private val _state = MutableStateFlow(QuizBrowseState())
    val state: StateFlow<QuizBrowseState> = _state.asStateFlow()

    fun select(stream: StudyStream) {
        _state.update { QuizBrowseState(streamId = stream.id, streamName = stream.name) }
    }

    fun clear() {
        _state.update { QuizBrowseState() }
    }
}