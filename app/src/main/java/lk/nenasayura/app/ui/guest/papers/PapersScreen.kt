package lk.nenasayura.app.ui.guest.papers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.paperTypeLabel
import lk.nenasayura.app.data.model.termLabel
import lk.nenasayura.app.ui.common.DocumentCard
import lk.nenasayura.app.core.session.UserSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.data.model.paperCategoryTitle

/** Lists A/L papers with filters. Works for guests and logged-in students. */
@Composable
fun PapersScreen(
    onBack: () -> Unit,
    viewModel: PapersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isGuest = currentSession().value is UserSession.Guest
    val errorMessage = state.errorMessage
    val visiblePapers = state.filteredPapers
    val categoryTitle = paperCategoryTitle(viewModel.category)

    Scaffold(
        topBar = { NenaTopBar(title = categoryTitle, onBack = onBack) },
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

                state.papers.isEmpty() -> EmptyView(
                    message = "No ${categoryTitle.lowercase()} are available yet."
                )

                else -> {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search ${categoryTitle.lowercase()}") },
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

                    if (state.availableSubjects.isNotEmpty()) {
                        // No "All" chip: one subject is always selected
                        FilterChipRow(
                            label = "Subject",
                            options = state.availableSubjects,
                            selected = state.availableSubjects
                                .firstOrNull { it.id == state.selectedSubjectId },
                            optionLabel = { it.name },
                            onSelected = { subject -> subject?.let { viewModel.onSubjectSelected(it.id) } },
                            showAll = false
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

                    // Province and Term filters are only for Provincial papers
                    if (viewModel.category == "provincial") {
                        if (state.availableProvinces.isNotEmpty()) {
                            FilterChipRow(
                                label = "Province",
                                options = state.availableProvinces,
                                selected = state.selectedProvince,
                                optionLabel = { it },
                                onSelected = viewModel::onProvinceSelected
                            )
                        }
                        if (state.availableTerms.isNotEmpty()) {
                            FilterChipRow(
                                label = "Term",
                                options = state.availableTerms,
                                selected = state.selectedTerm,
                                optionLabel = { termLabel(it) },
                                onSelected = viewModel::onTermSelected
                            )
                        }
                    }

                    if (visiblePapers.isEmpty()) {
                        EmptyView(
                            message = "No papers match your filters.",
                            actionLabel = if (state.hasActiveFilters) "Clear filters" else null,
                            onAction = if (state.hasActiveFilters) viewModel::clearFilters else null
                        )
                    } else {
                        // Group the papers by year, newest year first. Papers with no year go last.
                        val papersByYear = visiblePapers
                            .groupBy { it.year }
                            .toList()
                            .sortedByDescending { it.first }
                        val selectedSubjectName = state.availableSubjects
                            .firstOrNull { it.id == state.selectedSubjectId }?.name

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = listOfNotNull(
                                        selectedSubjectName,
                                        "${visiblePapers.size} papers"
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            papersByYear.forEach { (year, papersOfYear) ->
                                item(key = "year-$year") {
                                    YearHeader(year = year, count = papersOfYear.size)
                                }
                                items(papersOfYear, key = { it.id }) { paper ->
                                    // The year is already in the header above, so show the subject,
                                    // and the province + term for Provincial papers
                                    val subtitle = listOfNotNull(
                                        state.subjectName(paper.subjectId).ifBlank { null },
                                        paper.province.ifBlank { null },
                                        termLabel(paper.term).ifBlank { null }
                                    ).joinToString(" • ")

                                    DocumentCard(
                                        title = paper.title,
                                        subtitle = subtitle,
                                        badge = paper.type.takeIf { it.isNotBlank() }
                                            ?.let { paperTypeLabel(it) },
                                        onView = { FileActions.openFile(context, paper.fileUrl, paper.title) },
                                        onDownload = {
                                            FileActions.downloadFile(context, paper.fileUrl, paper.title)
                                        },
                                        showDownload = !isGuest
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "2021  ----------  3 papers" divider shown above each year's papers. */
@Composable
private fun YearHeader(year: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (year > 0) year.toString() else "Year not set",
            modifier = Modifier
                .background(NenaPrimaryContainer, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NenaPrimary
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (count == 1) "1 paper" else "$count papers",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}