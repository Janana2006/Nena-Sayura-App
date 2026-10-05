package lk.nenasayura.app.ui.student.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.theme.NenaIconColors

/** "What Our Students Say": the latest reviews from the database, as a horizontal carousel. */
@Composable
fun ReviewsSection(
    onViewAll: () -> Unit,
    onWriteReview: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage

    // Loads when the Home tab appears, and again after coming back from another screen
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = NenaIconColors.Reviews.tint
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "What Our Students Say",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Real experiences. Real stories.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.reviews.isEmpty() && errorMessage != null -> ReviewsMessage(
                title = "Could not load reviews",
                message = errorMessage,
                actionLabel = "Retry",
                onAction = viewModel::load
            )

            state.reviews.isEmpty() -> ReviewsMessage(
                title = "No reviews yet",
                message = "Be the first to share your experience.",
                actionLabel = "Write a review",
                onAction = onWriteReview
            )

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.reviews.take(MAX_ON_HOME), key = { it.id }) { review ->
                        ReviewCard(
                            review = review,
                            modifier = Modifier
                                .width(280.dp)
                                .heightIn(min = 170.dp),
                            maxCommentLines = 4
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onWriteReview) {
                        Text("Write a review", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onViewAll) {
                        Text("View All Reviews", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_ON_HOME = 10

@Composable
private fun ReviewsMessage(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}