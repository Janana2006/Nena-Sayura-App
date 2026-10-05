package lk.nenasayura.app.ui.student.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.Review
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EmptyStarColor = Color(0xFFB0B7C3)

/** Read-only row of 5 stars; the first [rating] are filled. */
@Composable
fun RatingStars(rating: Int, modifier: Modifier = Modifier, starSize: Dp = 16.dp) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            val filled = index < rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = if (filled) NenaIconColors.Reviews.tint else EmptyStarColor
            )
        }
    }
}

/** Five big stars the student taps to choose a rating. */
@Composable
fun StarRatingInput(rating: Int, onRatingChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            val filled = index < rating
            IconButton(onClick = { onRatingChange(index + 1) }) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "${index + 1} star",
                    modifier = Modifier.size(38.dp),
                    tint = if (filled) NenaIconColors.Reviews.tint else EmptyStarColor
                )
            }
        }
    }
}

/** One review: avatar initial, name, stars, comment and date. */
@Composable
fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
    maxCommentLines: Int = Int.MAX_VALUE
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NenaPrimaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = review.studentName.firstOrNull()?.uppercase() ?: "S",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NenaPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.studentName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RatingStars(rating = review.rating)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = maxCommentLines,
                overflow = TextOverflow.Ellipsis
            )

            if (review.createdAtMillis > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatReviewDate(review.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatReviewDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy • h:mm a", Locale.getDefault()).format(Date(millis))