package lk.nenasayura.app.ui.student.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import lk.nenasayura.app.data.repository.ReviewRepository

data class WriteReviewUiState(
    val rating: Int = 0,
    val comment: String = "",
    /** True when the student already wrote a review and is now editing it. */
    val isEditing: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSubmitted: Boolean = false
)

class WriteReviewViewModel : ViewModel() {

    private val reviewRepository = ReviewRepository()

    private val _uiState = MutableStateFlow(WriteReviewUiState())
    val uiState: StateFlow<WriteReviewUiState> = _uiState.asStateFlow()

    private var existingLoaded = false

    /** If this student already wrote a review, fills the form with it so it can be edited. */
    fun loadExisting(uid: String) {
        if (existingLoaded) return
        existingLoaded = true
        viewModelScope.launch {
            try {
                val review = reviewRepository.getReview(uid) ?: return@launch
                _uiState.update {
                    it.copy(rating = review.rating, comment = review.comment, isEditing = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The form simply starts empty if the old review cannot be loaded
            }
        }
    }

    fun onRatingChange(rating: Int) = _uiState.update { it.copy(rating = rating, errorMessage = null) }

    fun onCommentChange(value: String) =
        _uiState.update { it.copy(comment = value.take(MAX_COMMENT), errorMessage = null) }

    fun submit(uid: String, studentName: String) {
        val state = _uiState.value
        if (state.isSubmitting) return
        if (state.rating !in 1..5) {
            _uiState.update { it.copy(errorMessage = "Please choose a star rating.") }
            return
        }
        if (state.comment.trim().length < MIN_COMMENT) {
            _uiState.update { it.copy(errorMessage = "Please write a short comment (at least $MIN_COMMENT characters).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                // Without internet a write would wait forever, so give up after 15 seconds
                withTimeout(SUBMIT_TIMEOUT_MS) {
                    reviewRepository.saveReview(uid, studentName, state.rating, state.comment.trim())
                }
                _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Could not reach the server. Check your internet connection and try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Could not submit your review. Please try again."
                    )
                }
            }
        }
    }

    private companion object {
        const val MAX_COMMENT = 500
        const val MIN_COMMENT = 5
        const val SUBMIT_TIMEOUT_MS = 15_000L
    }
}