package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class ContentSection(val label: String) {
    Papers("Papers"),
    MarkingSchemes("Marking Schemes"),
    GuideBooks("Guide Books"),
    Quizzes("Quizzes"),
    Streams("Streams & Subjects")
}

/** Content tab: add papers, marking schemes, guide books, quizzes, streams and subjects. */
@Composable
fun AdminContentTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    var sectionIndex by rememberSaveable { mutableIntStateOf(0) }
    val section = ContentSection.entries[sectionIndex]

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))
        // Scrolls sideways because the chips do not all fit on a small phone
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContentSection.entries.forEachIndexed { index, s ->
                FilterChip(
                    selected = index == sectionIndex,
                    onClick = { sectionIndex = index },
                    label = { Text(s.label) }
                )
            }
        }

        when (section) {
            ContentSection.Papers -> PapersSection(state, viewModel)
            ContentSection.MarkingSchemes -> MarkingSchemesSection(state)
            ContentSection.GuideBooks -> GuideBooksSection(state, viewModel)
            ContentSection.Quizzes -> QuizzesSection(state)
            ContentSection.Streams -> StreamsSection(state, viewModel)
        }
    }
}