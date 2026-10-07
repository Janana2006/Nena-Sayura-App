package lk.nenasayura.app.ui.guest.papers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.core.browse.PaperBrowseState
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.CategoryScreen
import lk.nenasayura.app.ui.theme.NenaIconColors

/** Papers landing page: Past, Model and Provincial papers. [onOpenCategory] gets "past", "model" or "provincial". */
@Composable
fun PaperCategoriesScreen(
    onBack: () -> Unit,
    onOpenCategory: (category: String) -> Unit,
    // Students: the stream + subjects they picked (shown on top with a "Change" button).
    selection: PaperBrowseState? = null,
    onChangeSelection: (() -> Unit)? = null
) {
    CategoryScreen(
        title = "Papers",
        heading = "Choose a paper category",
        categories = listOf(
            CategoryItem(
                title = "Past Papers",
                subtitle = "Previous A/L exam papers",
                icon = Icons.Filled.History,
                palette = NenaIconColors.Papers,
                onClick = { onOpenCategory("past") }
            ),
            CategoryItem(
                title = "Model Papers",
                subtitle = "Practice papers for revision",
                icon = Icons.AutoMirrored.Filled.Assignment,
                palette = NenaIconColors.Quizzes,
                onClick = { onOpenCategory("model") }
            ),
            CategoryItem(
                title = "Provincial Papers",
                subtitle = "Papers from provincial exams",
                icon = Icons.Filled.LocationOn,
                palette = NenaIconColors.ResourceBooks,
                onClick = { onOpenCategory("provincial") }
            )
        ),
        onBack = onBack,
        header = if (selection != null && selection.hasSelection && onChangeSelection != null) {
            { SelectionSummary(selection, onChangeSelection) }
        } else null
    )
}

/** Small card: "Maths Stream - Combined Mathematics, Physics" + Change. */
@Composable
private fun SelectionSummary(selection: PaperBrowseState, onChange: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selection.streamName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selection.subjects.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onChange) { Text("Change") }
        }
    }
}