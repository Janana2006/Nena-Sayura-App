package lk.nenasayura.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.MarkingScheme
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.repository.MarkingSchemeAdminRepository

data class MarkingSchemeAdminUiState(
    val loading: Boolean = true,
    val schemes: List<MarkingScheme> = emptyList(),
    val loadError: String? = null,
    /** True while a scheme is being uploaded, saved or deleted. */
    val busy: Boolean = false
)

class MarkingSchemeAdminViewModel(
    private val repo: MarkingSchemeAdminRepository = MarkingSchemeAdminRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(MarkingSchemeAdminUiState())
    val state: StateFlow<MarkingSchemeAdminUiState> = _state.asStateFlow()

    private var loaded = false

    fun loadOnce() {
        if (loaded) return
        loaded = true
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val list = repo.getMarkingSchemes()
                _state.update { it.copy(loading = false, schemes = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Marking schemes load failed: ${e.localizedMessage}")
                _state.update {
                    it.copy(loading = false, loadError = "Could not load marking schemes: ${e.localizedMessage}")
                }
            }
        }
    }

    /** [onResult] gets null on success, or an error message to show in the dialog. */
    fun add(
        title: String,
        subject: Subject?,
        yearText: String,
        type: String,
        file: PickedFile?,
        link: String,
        onResult: (String?) -> Unit
    ) {
        val year = yearText.trim().toIntOrNull()
        val error = when {
            title.isBlank() -> "Enter a title."
            subject == null -> "Choose a subject."
            year == null || year < 1990 || year > 2100 -> "Enter a valid year (for example 2024)."
            file == null && !isLink(link) -> "Choose a file, or paste a link that starts with https://"
            else -> null
        }
        if (error != null || subject == null || year == null) {
            onResult(error)
            return
        }
        act(onResult) {
            val url = if (file != null) repo.uploadFile(type, file.uri, file.name) else link.trim()
            repo.addMarkingScheme(title, subject.id, year, type, url)
        }
    }

    fun delete(scheme: MarkingScheme, onResult: (String?) -> Unit) {
        act(onResult) { repo.deleteMarkingScheme(scheme.id) }
    }

    private fun isLink(url: String): Boolean {
        val u = url.trim()
        return u.startsWith("https://") || u.startsWith("http://")
    }

    private fun act(onResult: (String?) -> Unit, block: suspend () -> Unit) {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
                // Refresh the list; if only this refresh fails the save itself still worked
                val list = try {
                    repo.getMarkingSchemes()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                _state.update { it.copy(busy = false, schemes = list ?: it.schemes) }
                onResult(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false) }
                onResult(e.localizedMessage ?: "Something went wrong.")
            }
        }
    }

    private companion object {
        const val TAG = "MarkingSchemeVM"
    }
}