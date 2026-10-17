package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.core.planner.LoadSuggestion
import lk.nenasayura.app.core.planner.WeekReview
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer

/** The banner at the top of the Timetable when last week is ready to be reviewed. */
@Composable
fun ReviewBanner(isLoading: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = NenaAccentContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your weekly review is ready. See how last week went and update your plan.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF3E2A00)
            )
            TextButton(onClick = onOpen, enabled = !isLoading) {
                Text(if (isLoading) "Loading…" else "Review")
            }
        }
    }
}

/** Last week's result and what the plan will change. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewSheet(
    review: WeekReview,
    isApplying: Boolean,
    error: String?,
    onApply: (applySuggestion: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var applySuggestion by remember { mutableStateOf(false) }
    val boosted = review.subjects.filter { it.needsMoreTime }

    ModalBottomSheet(onDismissRequest = { if (!isApplying) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Weekly review",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${formatShortDate(review.weekStart)} - ${formatShortDate(review.weekStart.plusDays(6))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "${review.done} of ${review.total} sessions done",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { review.rate.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            if (review.unmarked > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${review.unmarked} session(s) were not marked, so they count as missed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (review.subjects.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "By subject",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                review.subjects.forEach { subject ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(subject.subjectName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${subject.done} of ${subject.total} done",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "What will change",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (review.carryOver.isNotEmpty()) {
                Bullet("${review.carryOver.size} missed session(s) are planned again in your next days.")
            }
            if (boosted.isNotEmpty()) {
                Bullet("More time for: ${boosted.joinToString { it.subjectName }}.")
            }
            if (review.carryOver.isEmpty() && boosted.isEmpty()) {
                Bullet("Nothing to catch up on. Your plan continues as it is.")
            }

            if (review.suggestion != LoadSuggestion.NONE) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = NenaPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (review.suggestion == LoadSuggestion.LIGHTER) {
                                "The last weeks were hard to keep up with. Make every study day one session lighter?"
                            } else {
                                "You are keeping up very well. Add one session to every study day?"
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1B1B1F)
                        )
                        Switch(checked = applySuggestion, onCheckedChange = { applySuggestion = it })
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !isApplying) { Text("Not now") }
                Button(onClick = { onApply(applySuggestion) }, enabled = !isApplying) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update my plan")
                    }
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text(
        text = "•  $text",
        modifier = Modifier.padding(vertical = 2.dp),
        style = MaterialTheme.typography.bodyMedium
    )
}