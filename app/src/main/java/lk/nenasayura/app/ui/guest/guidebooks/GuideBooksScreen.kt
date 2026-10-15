package lk.nenasayura.app.ui.guest.guidebooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.guideBookCategoryTitle
import lk.nenasayura.app.ui.common.DocumentCard
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.util.FileActions

/** Lists guide books. Guests get a subject filter, students open it for one chosen subject. */
@Composable
fun GuideBooksScreen(
    onBack: () -> Unit,
    viewModel: GuideBooksViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMessage = state.errorMessage
    val visibleBooks = state.filteredBooks
    val categoryTitle = guideBookCategoryTitle(viewModel.category)
    val badgeText = if (viewModel.category == "resource") "Resource Book" else "Guide Book"

    // Students: the subject they picked (stream -> category -> subject)
    val fixedSubjectName = state.fixedSubjectId
        ?.let { state.subjectName(it) }
        ?.ifBlank { null }
    val screenTitle = if (fixedSubjectName != null) "$fixedSubjectName • $categoryTitle" else categoryTitle

    Scaffold(
        topBar = { NenaTopBar(title = screenTitle, onBack = onBack) },
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

                state.books.isEmpty() -> EmptyView(
                    message = if (state.fixedSubjectId != null) {
                        "No ${categoryTitle.lowercase()} are available for this subject yet."
                    } else {
                        "No ${categoryTitle.lowercase()} are available yet."
                    }
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

                    // The subject filter is only for guests; a student already chose the subject
                    if (state.fixedSubjectId == null && state.availableSubjects.isNotEmpty()) {
                        FilterChipRow(
                            label = "Subject",
                            options = state.availableSubjects,
                            selected = state.availableSubjects
                                .firstOrNull { it.id == state.selectedSubjectId },
                            optionLabel = { it.name },
                            onSelected = { viewModel.onSubjectSelected(it?.id) }
                        )
                    }

                    if (visibleBooks.isEmpty()) {
                        EmptyView(
                            message = "No ${categoryTitle.lowercase()} match your filters.",
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
                                    text = "${visibleBooks.size} ${categoryTitle.lowercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(visibleBooks, key = { it.id }) { book ->
                                val subtitle = listOfNotNull(
                                    state.subjectName(book.subjectId).ifBlank { null },
                                    book.author.ifBlank { null }
                                ).joinToString(" • ")

                                DocumentCard(
                                    title = book.title,
                                    subtitle = subtitle,
                                    badge = badgeText,
                                    onView = { FileActions.openFile(context, book.fileUrl) },
                                    onDownload = {
                                        FileActions.downloadFile(context, book.fileUrl, book.title)
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