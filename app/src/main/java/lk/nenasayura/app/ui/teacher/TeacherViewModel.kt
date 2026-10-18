package lk.nenasayura.app.ui.teacher

import android.content.Context
import android.net.Uri
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
import lk.nenasayura.app.data.model.ContentStatus
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
    /** true while the new profile photo is being uploaded. */
    val photoUploading: Boolean = false,
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
                val newStatus = contentRepository.update(content, title, description, access)
                _state.update {
                    it.copy(
                        contents = it.contents.map { c ->
                            if (c.id == content.id) {
                                c.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    access = access,
                                    status = newStatus,
                                    declineReason = ""
                                )
                            } else c
                        },
                        message = if (newStatus == ContentStatus.PENDING) {
                            "Changes saved. Sent to the admin for approval again."
                        } else {
                            "Changes saved. Students can see them now."
                        }
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

    /** Saves the bank details from the Payments tab. All three filled, or all three empty. */
    fun saveBankDetails(bankName: String, accountNumber: String, branch: String, onSaved: () -> Unit) {
        val bank = bankName.trim()
        val account = accountNumber.trim()
        val bankBranch = branch.trim()
        val filled = listOf(bank, account, bankBranch).count { it.isNotEmpty() }
        val error = when {
            filled in 1..2 -> "Please fill the bank name, account number and branch."
            filled == 3 && account.length < 6 -> "Please enter a valid account number."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(message = error) }
            return
        }
        val uid = _state.value.profile?.uid ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                profileRepository.updateBankDetails(uid, bank, account, bankBranch)
                _state.update {
                    it.copy(
                        saving = false,
                        profile = it.profile?.copy(
                            bankName = bank,
                            accountNumber = account,
                            bankBranch = bankBranch
                        ),
                        message = if (filled == 0) "Bank details removed" else "Bank details saved"
                    )
                }
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, message = "Could not save bank details: ${e.localizedMessage}")
                }
            }
        }
    }

    /** The teacher took or chose a picture: shrink it, upload it and show it. */
    fun uploadPhoto(context: Context, uri: Uri) {
        val uid = _state.value.profile?.uid ?: return
        if (_state.value.photoUploading) return
        _state.update { it.copy(photoUploading = true) }
        viewModelScope.launch {
            try {
                val url = profileRepository.uploadProfilePhoto(context.applicationContext, uid, uri)
                _state.update {
                    it.copy(
                        photoUploading = false,
                        profile = it.profile?.copy(photoUrl = url),
                        message = "Profile photo updated"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        photoUploading = false,
                        message = "Could not upload the photo: " + (e.localizedMessage ?: "try again")
                    )
                }
            }
        }
    }

    fun removePhoto() {
        val uid = _state.value.profile?.uid ?: return
        viewModelScope.launch {
            try {
                profileRepository.removeProfilePhoto(uid)
                _state.update {
                    it.copy(profile = it.profile?.copy(photoUrl = ""), message = "Profile photo removed")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not remove the photo: ${e.localizedMessage}") }
            }
        }
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}