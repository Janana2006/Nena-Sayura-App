package lk.nenasayura.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.repository.RoleRepository
import lk.nenasayura.app.data.repository.SetupRepository
import lk.nenasayura.app.data.repository.StreamSeeder

data class SetupUiState(
    val role: String = "student",
    // true = the account already has the other role and this role is being ADDED
    val addRole: Boolean = false,
    val isFree: Boolean = true,
    val fee: Int = 0,
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

    // Student + stream ekata groups (slots) thiyenawa nam slots widihata select karanawa
    val hasGroups: Boolean
        get() = role != "teacher" && selectedStream?.groups?.isNotEmpty() == true

    // Teacher: kamathi subject ganak, Student: subjects 3k
    val maxSubjects: Int
        get() = if (role == "teacher") Int.MAX_VALUE else 3

    // Slots nathi stream walata: subjects 3t wada kamai nam thiyena okkoma select karanna oni
    val requiredCount: Int
        get() = if (hasGroups) {
            selectedStream?.groups?.size ?: 0
        } else {
            minOf(maxSubjects, selectedStream?.subjects?.size ?: maxSubjects)
        }

    val canConfirm: Boolean
        get() {
            val stream = selectedStream ?: return false
            return if (role == "teacher") {
                selectedSubjectIds.isNotEmpty()
            } else if (hasGroups) {
                // hama slot ekakama select karala thiyenna oni
                stream.groups.all { group -> group.any { it.id in selectedSubjectIds } }
            } else {
                requiredCount > 0 && selectedSubjectIds.size == requiredCount
            }
        }
}

class SetupViewModel(
    private val repo: SetupRepository = SetupRepository(),
    private val roleRepo: RoleRepository = RoleRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    // Screen eka open wena hama welawema fresh start ekak
    fun start(role: String = "teacher", addRole: Boolean = false, isFree: Boolean = true, fee: Int = 0) {
        _state.value = SetupUiState(role = role, addRole = addRole, isFree = isFree, fee = fee)
        loadStreams()
    }

    fun loadStreams() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (StreamSeeder.ENABLED) {
                try {
                    StreamSeeder.seedMissing()
                } catch (e: Exception) {
                    // Ignore seeding errors
                }
            }
            repo.getStreams { result ->
                result.fold(
                    onSuccess = { list ->
                        val fallbackMap = StreamSeeder.getFallbackStreams().associateBy { it.id }
                        val firestoreMap = list.associateBy { it.id }
                        val combinedKeys = (fallbackMap.keys + firestoreMap.keys)
                        val combined = combinedKeys.mapNotNull { id ->
                            val fs = firestoreMap[id]
                            val fb = fallbackMap[id]
                            if (fs != null && fs.subjects.isNotEmpty()) {
                                fs.copy(
                                    name = if (fs.name != fs.id && fs.name.isNotBlank()) fs.name else (fb?.name ?: fs.name),
                                    groups = if (fs.groups.isNotEmpty()) fs.groups else (fb?.groups ?: emptyList())
                                )
                            } else {
                                fb ?: fs
                            }
                        }.sortedBy { stream ->
                            when (stream.id.lowercase()) {
                                "maths" -> 1
                                "bio", "biology" -> 2
                                "commerce" -> 3
                                "technology" -> 4
                                "arts" -> 5
                                else -> 99
                            }
                        }
                        _state.update { it.copy(loading = false, streams = combined) }
                    },
                    onFailure = { e ->
                        val fallback = StreamSeeder.getFallbackStreams()
                        if (fallback.isNotEmpty()) {
                            _state.update { it.copy(loading = false, streams = fallback) }
                        } else {
                            _state.update {
                                it.copy(loading = false, error = e.localizedMessage ?: "Failed to load streams")
                            }
                        }
                    }
                )
            }
        }
    }

    fun selectStream(id: String) {
        if (_state.value.selectedStreamId == id) return

        _state.update { s ->
            val stream = s.streams.find { it.id == id }
            // Compulsory subjects (option 1k thiyena slots) kalinma select karanawa
            val preselected = if (s.role != "teacher" && stream != null) {
                stream.groups
                    .filter { it.size == 1 }
                    .map { it.first().id }
                    .toSet()
            } else {
                emptySet()
            }
            s.copy(selectedStreamId = id, selectedSubjectIds = preselected)
        }
    }

    fun toggleSubject(id: String) {
        _state.update { s ->
            val current = s.selectedSubjectIds
            val next = when {
                // Teacher: kamathi subject ganak select / unselect karanna puluwan
                s.role == "teacher" -> if (id in current) current - id else current + id

                // Student + slots
                s.hasGroups -> {
                    val group = s.selectedStream?.groups?.firstOrNull { g -> g.any { it.id == id } }
                    when {
                        group == null -> current
                        group.size == 1 -> current                                // compulsory, wenas karanna ba
                        else -> (current - group.map { it.id }.toSet()) + id      // slot eke palaweni eka ain wela aluth eka
                    }
                }

                // Student + slots nathi stream: 3t wada select karanna ba
                id in current -> current - id
                current.size < s.maxSubjects -> current + id
                else -> current
            }
            s.copy(selectedSubjectIds = next)
        }
    }

    fun confirm() {
        val s = _state.value
        val stream = s.selectedStream ?: return
        if (!s.canConfirm) return

        // Slots thiyenawa nam slot order eken (Subject 1, 2, 3)
        val chosen = if (s.hasGroups) {
            stream.groups.mapNotNull { group -> group.firstOrNull { it.id in s.selectedSubjectIds } }
        } else {
            stream.subjects.filter { it.id in s.selectedSubjectIds }
        }

        _state.update { it.copy(saving = true, error = null) }

        if (s.addRole) {
            // Existing account getting a second role: do not overwrite the first role's details
            viewModelScope.launch {
                try {
                    roleRepo.addRole(s.role, stream, chosen, s.isFree, s.fee)
                    _state.update { it.copy(saving = false, done = true) }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _state.update { it.copy(saving = false, error = e.localizedMessage ?: "Failed to save") }
                }
            }
            return
        }

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