package lk.nenasayura.app.ui.student.reviews

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.session.UserSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.ui.common.NenaTopBar

/** A student rates the app and writes a short review. Writing again edits the earlier review. */
@Composable
fun WriteReviewScreen(
    onBack: () -> Unit,
    viewModel: WriteReviewViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session by currentSession()
    val student = session as? UserSession.LoggedIn
    val context = LocalContext.current

    // Fill the form with the student's earlier review, if there is one
    LaunchedEffect(student?.uid) {
        student?.uid?.let { viewModel.loadExisting(it) }
    }

    // After a successful submit: thank the student and go back
    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) {
            Toast.makeText(context, "Thank you for your review!", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    val ratingLabel = when (state.rating) {
        1 -> "Poor"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Very good"
        5 -> "Excellent"
        else -> "Tap a star to rate"
    }
    val errorMessage = state.errorMessage

    Scaffold(
        topBar = {
            NenaTopBar(
                title = if (state.isEditing) "Edit your review" else "Write a review",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "How is your experience with Nena Sayura?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            StarRatingInput(rating = state.rating, onRatingChange = viewModel::onRatingChange)
            Text(
                text = ratingLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = state.comment,
                onValueChange = viewModel::onCommentChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your review") },
                supportingText = { Text("${state.comment.length}/500") },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(14.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { student?.let { viewModel.submit(it.uid, it.name) } },
                enabled = student != null && !state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (state.isEditing) "Update review" else "Submit review")
                }
            }
        }
    }
}