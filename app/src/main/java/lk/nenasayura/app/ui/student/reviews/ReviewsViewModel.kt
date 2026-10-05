package lk.nenasayura.app.ui.student.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.data.model.Review
import lk.nenasayura.app.data.repository.ReviewRepository

data class ReviewsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val reviews: List<Review> = emptyList()
) {
    val averageRating: Double
        get() = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
}

class ReviewsViewModel : ViewModel() {

    private val reviewRepository = ReviewRepository()

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    /** Loads the reviews. Call it again to refresh (for example after writing a review). */
    fun load() {
        viewModelScope.launch {
            // The spinner shows only the first time; a refresh keeps the old reviews visible
            _uiState.update { it.copy(isLoading = it.reviews.isEmpty(), errorMessage = null) }
            try {
                val reviews = reviewRepository.getReviews()
                _uiState.update { it.copy(isLoading = false, reviews = reviews) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load reviews. Check your internet connection and try again."
                    )
                }
            }
        }
    }
}