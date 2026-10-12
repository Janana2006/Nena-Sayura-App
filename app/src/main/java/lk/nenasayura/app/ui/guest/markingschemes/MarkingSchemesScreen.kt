package lk.nenasayura.app.ui.guest.markingschemes



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.common.DocumentCard
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.data.model.markingSchemeCategoryTitle

/** Lists marking schemes with filters. Works for guests and logged-in students. */
@Composable
fun MarkingSchemesScreen(
    onBack: () -> Unit,
    viewModel: MarkingSchemesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMessage = state.errorMessage
    val visibleSchemes = state.filteredSchemes
    val categoryTitle = markingSchemeCategoryTitle(viewModel.category)

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

                state.schemes.isEmpty() -> EmptyView(
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

                    if (visibleSchemes.isEmpty()) {
                        EmptyView(
                            message = "No marking schemes match your filters.",
                            actionLabel = if (state.hasActiveFilters) "Clear filters" else null,
                            onAction = if (state.hasActiveFilters) viewModel::clearFilters else null
                        )
                    } else {
                        // Group the marking schemes by year, newest year first. No year goes last.
                        val schemesByYear = visibleSchemes
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
                                        "${visibleSchemes.size} marking schemes"
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            schemesByYear.forEach { (year, schemesOfYear) ->
                                item(key = "year-$year") {
                                    YearHeader(year = year, count = schemesOfYear.size)
                                }
                                items(schemesOfYear, key = { it.id }) { scheme ->
                                    DocumentCard(
                                        title = scheme.title,
                                        // The year is already in the header above, so show only the subject
                                        subtitle = state.subjectName(scheme.subjectId),
                                        badge = "Marking Scheme",
                                        onView = { FileActions.openFile(context, scheme.fileUrl, scheme.title) },
                                        onDownload = {
                                            FileActions.downloadFile(context, scheme.fileUrl, scheme.title)
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
}

/** "2021  ----------  3 marking schemes" divider shown above each year's schemes. */
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
            text = if (count == 1) "1 scheme" else "$count schemes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}