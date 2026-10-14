package lk.nenasayura.app.ui.guest.guidebooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.core.browse.GuideBrowseSelection
import lk.nenasayura.app.data.model.guideBookCategoryTitle
import lk.nenasayura.app.ui.common.CategoryCard
import lk.nenasayura.app.ui.common.CategoryItem
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaIconColors

/**
 * Step 3 of Guide Books (students): the subjects of the chosen stream.
 * [category] is "guide" or "resource".
 */
@Composable
fun GuideSubjectsScreen(
    category: String,
    onBack: () -> Unit,
    onOpenSubject: (subjectId: String) -> Unit
) {
    val selection by GuideBrowseSelection.state.collectAsStateWithLifecycle()
    val categoryTitle = guideBookCategoryTitle(category)
    val palette = if (category == "resource") NenaIconColors.ResourceBooks else NenaIconColors.GuideBooks

    Scaffold(
        topBar = { NenaTopBar(title = categoryTitle, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selection.subjects.isEmpty()) {
                EmptyView(message = "No subjects are available for this stream yet.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Choose a subject",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selection.streamName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(selection.subjects, key = { it.id }) { subject ->
                        CategoryCard(
                            CategoryItem(
                                title = subject.name,
                                subtitle = categoryTitle,
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                palette = palette,
                                onClick = { onOpenSubject(subject.id) }
                            )
                        )
                    }
                }
            }
        }
    }
}