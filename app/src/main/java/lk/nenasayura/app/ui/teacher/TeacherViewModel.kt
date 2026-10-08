package lk.nenasayura.app.ui.teacher

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.data.repository.SubjectRepository
import lk.nenasayura.app.data.repository.TeacherContentRepository
import lk.nenasayura.app.data.repository.TeacherProfileRepository

data class TeacherUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val profile: TeacherProfile? = null,
    val contents: List<TeacherContent> = emptyList(),
    val contentRefreshing: Boolean = false,
    /** Subjects of the teacher's stream, offered when editing the profile. */
    val streamSubjects: List<Subject> = emptyList(),
    val saving: Boolean = false,
    /** One-time message shown in a snackbar. */
    val message: String? = null
)

/** Holds the teacher's profile and uploads; shared by every tab of the Teacher Home. */
class TeacherViewModel(
    private val profileRepository: TeacherProfileRepository = TeacherProfileRepository(),
    private val contentRepository: TeacherContentRepository = TeacherContentRepository(),
    private val subjectRepository: SubjectRepository = SubjectRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherUiState())
    val state: StateFlow<TeacherUiState> = _state.asStateFlow()

    private var loadedFor: String? = null
    private var fallbackName = ""
    private var fallbackEmail = ""

    /** Called once when the Teacher Home opens (a second call for the same teacher does nothing). */
    fun start(uid: String, name: String, email: String) {
        if (loadedFor == uid) return
        loadedFor = uid
        fallbackName = name
        fallbackEmail = email
        loadAll()
    }

    fun reload() = loadAll()

    private fun loadAll() {
        val uid = loadedFor ?: return
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            // Profile read karanna ba nam, login eke name / email walin screen eka pennanawa
            val profile = try {
                profileRepository.getProfile(uid, fallbackName, fallbackEmail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TeacherVM", "Profile load failed: ${e.localizedMessage}")
                TeacherProfile(uid = uid, name = fallbackName, email = fallbackEmail)
            }

            // Uploads read karanna ba nam, empty list ekak pennanawa
            val contents = try {
                contentRepository.getContents(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TeacherVM", "Uploads load failed: ${e.localizedMessage}")
                emptyList()
            }

            _state.update {
                it.copy(loading = false, profile = profile, contents = contents)
            }
        }
    }

    /** Reloads only the uploads (after an upload, edit or delete). */
    fun refreshContent(message: String? = null) {
        val uid = loadedFor ?: return
        _state.update { it.copy(contentRefreshing = true) }
        viewModelScope.launch {
            try {
                val contents = contentRepository.getContents(uid)
                _state.update {
                    it.copy(
                        contents = contents,
                        contentRefreshing = false,
                        message = message ?: it.message
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        contentRefreshing = false,
                        message = "Could not refresh: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun deleteContent(content: TeacherContent) {
        viewModelScope.launch {
            try {
                contentRepository.delete(content)
                _state.update {
                    it.copy(
                        contents = it.contents.filterNot { c -> c.id == content.id },
                        message = "Deleted \"${content.title}\""
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not delete: ${e.localizedMessage}") }
            }
        }
    }

    fun updateContent(content: TeacherContent, title: String, description: String, access: AccessType) {
        if (title.isBlank()) {
            _state.update { it.copy(message = "The title cannot be empty.") }
            return
        }
        viewModelScope.launch {
            try {
                contentRepository.update(content.id, title, description, access)
                _state.update {
                    it.copy(
                        contents = it.contents.map { c ->
                            if (c.id == content.id) {
                                c.copy(title = title.trim(), description = description.trim(), access = access)
                            } else c
                        },
                        message = "Changes saved"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not save: ${e.localizedMessage}") }
            }
        }
    }

    /** Loads the subjects of the teacher's stream (for the profile editor). */
    fun loadStreamSubjects() {
        val streamId = _state.value.profile?.streamId.orEmpty()
        if (streamId.isBlank() || _state.value.streamSubjects.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val list = subjectRepository.getSubjectsByStream(streamId)
                _state.update { it.copy(streamSubjects = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not load subjects: ${e.localizedMessage}") }
            }
        }
    }

    fun saveProfile(updated: TeacherProfile, onSaved: () -> Unit) {
        val error = when {
            updated.name.isBlank() -> "Please enter your name."
            updated.subjectIds.isEmpty() -> "Please select at least one subject."
            !updated.isFree && updated.fee <= 0 -> "Please enter a valid fee."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(message = error) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                profileRepository.updateProfile(updated)
                _state.update {
                    it.copy(saving = false, profile = updated, message = "Profile updated")
                }
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, message = "Could not save profile: ${e.localizedMessage}")
                }
            }
        }
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}