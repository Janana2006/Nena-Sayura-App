package lk.nenasayura.app.ui.student.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaIconColors

/** Every review in the database, newest first. */
@Composable
fun AllReviewsScreen(
    onBack: () -> Unit,
    onWriteReview: () -> Unit,
    viewModel: ReviewsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Scaffold(
        topBar = {
            NenaTopBar(
                title = "Student Reviews",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onWriteReview) {
                        Icon(Icons.Filled.Edit, contentDescription = "Write a review")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(modifier = Modifier.padding(innerPadding))

            state.reviews.isEmpty() && errorMessage != null -> ErrorView(
                message = errorMessage,
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding)
            )

            state.reviews.isEmpty() -> EmptyView(
                message = "No reviews yet. Be the first to share your experience.",
                actionLabel = "Write a review",
                onAction = onWriteReview,
                modifier = Modifier.padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = NenaIconColors.Reviews.tint
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%.1f".format(state.averageRating) +
                                    "  •  ${state.reviews.size} reviews",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                items(state.reviews, key = { it.id }) { review ->
                    ReviewCard(review = review, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}