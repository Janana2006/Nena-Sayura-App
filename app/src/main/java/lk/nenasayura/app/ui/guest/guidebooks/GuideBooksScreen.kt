package lk.nenasayura.app.ui.guest.guidebooks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.guideBookCategoryTitle
import lk.nenasayura.app.ui.common.DocumentCard
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.HeroBanner
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.util.FileActions

/**
 * Lists guide books / resource books.
 * - Guests: only the subjects they chose (chips, no "All"), and only the ones that have books.
 * - Students: the books of the one subject they opened.
 */
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
    val side = Modifier.padding(horizontal = 16.dp)

    // Students: the subject they picked (stream -> category -> subject)
    val fixedSubjectName = state.fixedSubjectId
        ?.let { state.subjectName(it) }
        ?.ifBlank { null }
    val screenTitle = if (fixedSubjectName != null) "$fixedSubjectName • $categoryTitle" else categoryTitle

    Scaffold(
        topBar = { NenaTopBar(title = screenTitle, onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
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

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        HeroBanner(
                            title = categoryTitle,
                            subtitle = when {
                                fixedSubjectName != null -> fixedSubjectName
                                state.restricted -> "Books for your subjects"
                                else -> "Study books for every subject"
                            },
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            modifier = side
                        )
                    }

                    if (state.books.isEmpty()) {
                        item {
                            Text(
                                text = when {
                                    state.fixedSubjectId != null ->
                                        "No ${categoryTitle.lowercase()} are available for this subject yet."
                                    state.restricted ->
                                        "No ${categoryTitle.lowercase()} have been added for your subjects yet."
                                    else -> "No ${categoryTitle.lowercase()} are available yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 32.dp)
                            )
                        }
                    } else {
                        item {
                            OutlinedTextField(
                                value = state.query,
                                onValueChange = viewModel::onQueryChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
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
                                shape = CircleShape
                            )
                        }

                        // Subject chips are only for guests; a student already chose the subject.
                        // Guests who chose subjects get no "All" chip, only their subjects that have books.
                        if (state.fixedSubjectId == null && state.availableSubjects.isNotEmpty()) {
                            item {
                                FilterChipRow(
                                    label = if (state.restricted) "Your subjects" else "Subject",
                                    options = state.availableSubjects,
                                    selected = state.availableSubjects
                                        .firstOrNull { it.id == state.selectedSubjectId },
                                    optionLabel = { it.name },
                                    onSelected = { subject ->
                                        if (subject != null || !state.restricted) {
                                            viewModel.onSubjectSelected(subject?.id)
                                        }
                                    },
                                    showAll = !state.restricted
                                )
                            }
                        }

                        if (visibleBooks.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No ${categoryTitle.lowercase()} match your search.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (state.hasActiveFilters) {
                                        TextButton(onClick = viewModel::clearFilters) {
                                            Text("Clear search")
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(
                                    text = "${visibleBooks.size} ${categoryTitle.lowercase()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = side
                                )
                            }
                            items(visibleBooks, key = { it.id }) { book ->
                                // The chosen subject chip is already on screen for guests
                                val subtitle = listOfNotNull(
                                    state.subjectName(book.subjectId)
                                        .ifBlank { null }
                                        ?.takeIf { !state.restricted },
                                    book.author.ifBlank { null }
                                ).joinToString(" • ")

                                Box(modifier = side) {
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
}