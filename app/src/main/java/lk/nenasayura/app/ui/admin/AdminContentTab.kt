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
import lk.nenasayura.app.data.model.AdminPermission

private enum class ContentSection(val label: String, val permission: AdminPermission) {
    Papers("Papers", AdminPermission.PAPERS),
    MarkingSchemes("Marking Schemes", AdminPermission.MARKING_SCHEMES),
    GuideBooks("Guide Books", AdminPermission.GUIDE_BOOKS),
    Quizzes("Quizzes", AdminPermission.QUIZZES),
    Streams("Streams & Subjects", AdminPermission.STREAMS)
}

/** Content tab: add papers, marking schemes, guide books, quizzes, streams and subjects. */
@Composable
fun AdminContentTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    // Only the sections this admin was given access to
    val sections = ContentSection.entries.filter { state.can(it.permission) }
    var sectionIndex by rememberSaveable { mutableIntStateOf(0) } // ordinal of the section
    val section = ContentSection.entries[sectionIndex].takeIf { it in sections }
        ?: sections.firstOrNull()
        ?: return

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))
        // Scrolls sideways because the chips do not all fit on a small phone
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEach { s ->
                FilterChip(
                    selected = s == section,
                    onClick = { sectionIndex = s.ordinal },
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