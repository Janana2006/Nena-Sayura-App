package lk.nenasayura.app.ui.student.papers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.repository.SubjectRepository

data class StreamSelectUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val streams: List<StudyStream> = emptyList()
)

/** Loads the streams from Firestore for the "Choose your stream" screen. */
class StreamSelectViewModel : ViewModel() {

    private val repository = SubjectRepository()

    private val _uiState = MutableStateFlow(StreamSelectUiState())
    val uiState: StateFlow<StreamSelectUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val streams = repository.getStreams()
                _uiState.update { it.copy(isLoading = false, streams = streams) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load streams. Check your internet connection and try again."
                    )
                }
            }
        }
    }
}