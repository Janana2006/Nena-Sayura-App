package lk.nenasayura.app.ui.guest.search



import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.paperTypeLabel
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.util.FileActions

/** One row in the search results (a subject, a document or a quiz). */
private data class SearchResultItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val onDownload: (() -> Unit)? = null
)

/** Searches subjects and all educational resources. Works for guests and students. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenQuiz: (quizId: String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val errorMessage = state.errorMessage

    // Open the keyboard as soon as the screen appears
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = { NenaTopBar(title = "Search", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Search subjects, papers, quizzes…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            when {
                state.isLoading -> LoadingView()

                errorMessage != null -> ErrorView(
                    message = errorMessage,
                    onRetry = viewModel::load
                )

                else -> {
                    if (state.streams.isNotEmpty()) {
                        FilterChipRow(
                            label = "Stream",
                            options = state.streams,
                            selected = state.selectedStream,
                            optionLabel = { it },
                            onSelected = viewModel::onStreamSelected
                        )
                    }

                    state.selectedSubject?.let { subject ->
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onSubjectSelected(null) },
                                label = { Text("Subject: ${subject.name}") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove subject filter",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }

                    SearchResults(
                        state = state,
                        context = context,
                        onSubjectSelected = viewModel::onSubjectSelected,
                        onOpenQuiz = onOpenQuiz
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    state: SearchUiState,
    context: Context,
    onSubjectSelected: (String?) -> Unit,
    onOpenQuiz: (String) -> Unit
) {
    val subjectItems = state.subjectResults.map { subject ->
        SearchResultItem(
            key = "subject-${subject.id}",
            title = subject.name,
            subtitle = subject.stream,
            icon = Icons.Filled.School,
            onClick = { onSubjectSelected(subject.id) }
        )
    }

    val paperItems = state.paperResults.map { paper ->
        SearchResultItem(
            key = "paper-${paper.id}",
            title = paper.title,
            subtitle = listOfNotNull(
                state.subjectName(paper.subjectId).ifBlank { null },
                paper.year.takeIf { it > 0 }?.toString(),
                paper.type.takeIf { it.isNotBlank() }?.let { paperTypeLabel(it) }
            ).joinToString(" • "),
            icon = Icons.Filled.Description,
            onClick = { FileActions.openFile(context, paper.fileUrl) },
            onDownload = { FileActions.downloadFile(context, paper.fileUrl, paper.title) }
        )
    }

    val schemeItems = state.markingSchemeResults.map { scheme ->
        SearchResultItem(
            key = "scheme-${scheme.id}",
            title = scheme.title,
            subtitle = listOfNotNull(
                state.subjectName(scheme.subjectId).ifBlank { null },
                scheme.year.takeIf { it > 0 }?.toString()
            ).joinToString(" • "),
            icon = Icons.Filled.FactCheck,
            onClick = { FileActions.openFile(context, scheme.fileUrl) },
            onDownload = { FileActions.downloadFile(context, scheme.fileUrl, scheme.title) }
        )
    }

    val bookItems = state.guideBookResults.map { book ->
        SearchResultItem(
            key = "book-${book.id}",
            title = book.title,
            subtitle = listOfNotNull(
                state.subjectName(book.subjectId).ifBlank { null },
                book.author.ifBlank { null }
            ).joinToString(" • "),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = { FileActions.openFile(context, book.fileUrl) },
            onDownload = { FileActions.downloadFile(context, book.fileUrl, book.title) }
        )
    }

    val quizItems = state.quizResults.map { quiz ->
        SearchResultItem(
            key = "quiz-${quiz.id}",
            title = quiz.title,
            subtitle = state.subjectName(quiz.subjectId),
            icon = Icons.Filled.Quiz,
            onClick = { onOpenQuiz(quiz.id) }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Subject list: shown for browsing, hidden once a single subject is chosen
        if (state.selectedSubjectId == null) {
            resultSection(
                title = if (state.hasCriteria) "Subjects" else "Browse subjects",
                items = subjectItems
            )
        }

        if (state.hasCriteria) {
            resultSection("Papers", paperItems)
            resultSection("Marking Schemes", schemeItems)
            resultSection("Guide Books", bookItems)
            resultSection("Quizzes", quizItems)
        }

        val nothingFound = if (state.hasCriteria) {
            state.resourceCount == 0 && (state.selectedSubjectId != null || subjectItems.isEmpty())
        } else {
            subjectItems.isEmpty()
        }
        if (nothingFound) {
            item(key = "nothing-found") {
                Text(
                    text = when {
                        !state.hasCriteria -> "No subjects are available yet."
                        state.selectedSubjectId != null -> "No resources found for this subject yet."
                        else -> "No results found. Try a different word or stream."
                    },
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun LazyListScope.resultSection(title: String, items: List<SearchResultItem>) {
    if (items.isEmpty()) return
    item(key = "header-$title") {
        Text(
            text = "$title (${items.size})",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    items(items, key = { it.key }) { item -> SearchResultRow(item) }
}

@Composable
private fun SearchResultRow(item: SearchResultItem) {
    Card(
        onClick = item.onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NenaPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = NenaPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            item.onDownload?.let { download ->
                IconButton(onClick = download) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = NenaPrimary
                    )
                }
            }
        }
    }
}