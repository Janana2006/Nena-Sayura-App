package lk.nenasayura.app.ui.guest.papers



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.paperTypeLabel
import lk.nenasayura.app.ui.common.DocumentCard
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.util.FileActions

/** Lists A/L papers with filters. Works for guests and logged-in students. */
@Composable
fun PapersScreen(
    onBack: () -> Unit,
    viewModel: PapersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMessage = state.errorMessage
    val visiblePapers = state.filteredPapers

    Scaffold(
        topBar = { NenaTopBar(title = "Papers", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> LoadingView()

                errorMessage != null -> ErrorView(
                    message = errorMessage,
                    onRetry = viewModel::load
                )

                state.papers.isEmpty() -> EmptyView(message = "No papers are available yet.")

                else -> {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search by title or subject") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (state.availableTypes.isNotEmpty()) {
                        FilterChipRow(
                            label = "Paper type",
                            options = state.availableTypes,
                            selected = state.selectedType,
                            optionLabel = { paperTypeLabel(it) },
                            onSelected = viewModel::onTypeSelected
                        )
                    }
                    if (state.availableSubjects.isNotEmpty()) {
                        FilterChipRow(
                            label = "Subject",
                            options = state.availableSubjects,
                            selected = state.availableSubjects
                                .firstOrNull { it.id == state.selectedSubjectId },
                            optionLabel = { it.name },
                            onSelected = { viewModel.onSubjectSelected(it?.id) }
                        )
                    }
                    if (state.availableYears.isNotEmpty()) {
                        FilterChipRow(
                            label = "Year",
                            options = state.availableYears,
                            selected = state.selectedYear,
                            optionLabel = { it.toString() },
                            onSelected = viewModel::onYearSelected
                        )
                    }

                    if (visiblePapers.isEmpty()) {
                        EmptyView(
                            message = "No papers match your filters.",
                            actionLabel = if (state.hasActiveFilters) "Clear filters" else null,
                            onAction = if (state.hasActiveFilters) viewModel::clearFilters else null
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "${visiblePapers.size} papers",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(visiblePapers, key = { it.id }) { paper ->
                                val subtitle = listOfNotNull(
                                    state.subjectName(paper.subjectId).ifBlank { null },
                                    paper.year.takeIf { it > 0 }?.toString()
                                ).joinToString(" • ")

                                DocumentCard(
                                    title = paper.title,
                                    subtitle = subtitle,
                                    badge = paper.type.takeIf { it.isNotBlank() }
                                        ?.let { paperTypeLabel(it) },
                                    onView = { FileActions.openFile(context, paper.fileUrl) },
                                    onDownload = {
                                        FileActions.downloadFile(context, paper.fileUrl, paper.title)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}