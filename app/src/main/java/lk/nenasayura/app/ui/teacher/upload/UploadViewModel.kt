package lk.nenasayura.app.ui.teacher.upload

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.SubjectRepository
import lk.nenasayura.app.data.repository.TeacherContentRepository

data class UploadUiState(
    val type: ContentType = ContentType.SHORT_NOTE,
    val title: String = "",
    val description: String = "",
    val subjects: List<Subject> = emptyList(),
    val subjectsLoading: Boolean = true,
    val selectedSubject: Subject? = null,
    val access: AccessType = AccessType.FREE,
    val fileUri: Uri? = null,
    val fileName: String = "",
    val videoUrl: String = "",
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    val uploadedOk: Boolean = false
)

class UploadViewModel(
    private val subjectRepository: SubjectRepository = SubjectRepository(),
    private val contentRepository: TeacherContentRepository = TeacherContentRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(UploadUiState())
    val state: StateFlow<UploadUiState> = _state.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            try {
                val list = subjectRepository.getSubjects()
                _state.update { it.copy(subjects = list, subjectsLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        subjectsLoading = false,
                        errorMessage = "Could not load subjects: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    // ---- form changes (each one also clears old error / success messages) ----

    fun onTypeChange(type: ContentType) = edit { it.copy(type = type) }
    fun onTitleChange(value: String) = edit { it.copy(title = value) }
    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }
    fun onSubjectSelected(subject: Subject) = edit { it.copy(selectedSubject = subject) }
    fun onAccessChange(access: AccessType) = edit { it.copy(access = access) }
    fun onVideoUrlChange(value: String) = edit { it.copy(videoUrl = value) }

    fun onFilePicked(uri: Uri, name: String, sizeBytes: Long) {
        if (sizeBytes > MAX_FILE_BYTES) {
            _state.update {
                it.copy(errorMessage = "File is too large. Maximum size is 50 MB.", uploadedOk = false)
            }
            return
        }
        edit { it.copy(fileUri = uri, fileName = name) }
    }

    private fun edit(change: (UploadUiState) -> UploadUiState) {
        _state.update { change(it).copy(errorMessage = null, uploadedOk = false) }
    }

    // ---- upload ----

    fun submit(teacherId: String, teacherName: String) {
        val s = _state.value
        if (s.isUploading) return

        val subject = s.selectedSubject
        val error = when {
            s.title.isBlank() -> "Please enter a title."
            subject == null -> "Please select a subject."
            s.type == ContentType.VIDEO_LINK && !isValidLink(s.videoUrl) ->
                "Please enter a valid video link (starting with https://)."
            s.type != ContentType.VIDEO_LINK && s.fileUri == null ->
                "Please choose a file to upload."
            else -> null
        }
        if (error != null || subject == null) {
            _state.update { it.copy(errorMessage = error, uploadedOk = false) }
            return
        }

        _state.update { it.copy(isUploading = true, errorMessage = null, uploadedOk = false) }
        viewModelScope.launch {
            try {
                contentRepository.upload(
                    teacherId = teacherId,
                    teacherName = teacherName,
                    title = s.title,
                    description = s.description,
                    subject = subject,
                    type = s.type,
                    access = s.access,
                    fileUri = s.fileUri,
                    fileName = s.fileName,
                    videoUrl = s.videoUrl
                )
                // Clear the form but keep type / subject / access for the next upload.
                _state.update {
                    it.copy(
                        title = "",
                        description = "",
                        fileUri = null,
                        fileName = "",
                        videoUrl = "",
                        isUploading = false,
                        uploadedOk = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isUploading = false,
                        errorMessage = "Upload failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun isValidLink(url: String): Boolean {
        val u = url.trim()
        return u.startsWith("https://") || u.startsWith("http://")
    }

    private companion object {
        const val MAX_FILE_BYTES = 50L * 1024 * 1024
    }
}