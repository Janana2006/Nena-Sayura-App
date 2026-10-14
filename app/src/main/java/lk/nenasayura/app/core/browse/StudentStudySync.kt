package lk.nenasayura.app.core.browse

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.StudentProfileRepository

/**
 * Connects the stream + subjects a student chose at sign-up (saved in the profile)
 * with Papers, Marking Schemes and Guide Books.
 *
 * Students no longer pick a stream every time they open those screens. The saved
 * choice is loaded once and put into [PaperBrowseSelection] and [GuideBrowseSelection],
 * so every list only shows the student's own subjects.
 */
object StudentStudySync {

    private val _version = MutableStateFlow(0)

    /** Goes up every time the student changes the stream/subjects (the profile tab listens to this). */
    val version: StateFlow<Int> = _version.asStateFlow()

    /**
     * Reads the student's saved stream + subjects and applies them.
     * Returns false when the student has not chosen any subjects yet.
     * Throws when the profile cannot be loaded (for example no internet).
     */
    suspend fun load(repository: StudentProfileRepository = StudentProfileRepository()): Boolean {
        val profile = repository.getProfile()

        if (!profile.hasStudyDetails || profile.streamId.isBlank() || profile.subjectIds.isEmpty()) {
            PaperBrowseSelection.clear()
            GuideBrowseSelection.clear()
            return false
        }

        // subjectIds and subjectNames are saved in the same order
        val subjects = profile.subjectIds.mapIndexed { index, id ->
            Subject(id = id, name = profile.subjectNames.getOrNull(index) ?: id)
        }
        val stream = StudyStream(
            id = profile.streamId,
            name = profile.streamName,
            subjects = subjects
        )

        PaperBrowseSelection.select(stream, subjects)
        GuideBrowseSelection.select(stream)
        return true
    }

    /**
     * Call this after the student saved a new stream/subjects.
     * The old choice is dropped, and the next screen reloads the new one from the profile.
     */
    fun onStudyChanged() {
        PaperBrowseSelection.clear()
        GuideBrowseSelection.clear()
        _version.update { it + 1 }
    }
}