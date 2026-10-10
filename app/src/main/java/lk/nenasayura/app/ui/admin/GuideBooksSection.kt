package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.GuideBook
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.guideBookCategoryTitle
import lk.nenasayura.app.data.model.isInCategory
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.util.FileActions

private val BOOK_CATEGORIES = listOf(
    "guide" to "Guide Books",
    "resource" to "Resource Books"
)

/** Guide books and resource books. Add new ones, open or delete the existing ones. */
@Composable
fun GuideBooksSection(state: AdminUiState, viewModel: AdminViewModel) {
    val context = LocalContext.current
    var category by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<GuideBook?>(null) }

    val shown = state.guideBooks.filter { category == null || it.isInCategory(category!!) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add guide book")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("All (${state.guideBooks.size})") }
                )
                BOOK_CATEGORIES.forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item { EmptyView(message = "No books here yet.", modifier = Modifier.height(160.dp)) }
        } else {
            items(shown, key = { it.id }) { book ->
                AdminCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = listOf(state.subjectName(book.subjectId), book.author)
                                .filter { it.isNotBlank() }
                                .joinToString(" • ")
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Pill(
                                guideBookCategoryTitle(book.category.ifBlank { "guide" }),
                                NenaAccentContainer,
                                NenaPrimary
                            )
                        }
                        IconButton(onClick = { FileActions.openFile(context, book.fileUrl) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = NenaPrimary)
                        }
                        IconButton(onClick = { deleting = book }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddGuideBookDialog(
            subjects = state.allSubjects,
            busy = state.busy,
            viewModel = viewModel,
            onDismiss = { showAdd = false }
        )
    }

    deleting?.let { book ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this book?") },
            text = { Text("\"${book.title}\" will be removed and students will no longer see it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGuideBook(book)
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddGuideBookDialog(
    subjects: List<Subject>,
    busy: Boolean,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("guide") }
    var subject by remember { mutableStateOf<Subject?>(null) }
    var file by remember { mutableStateOf<PickedFile?>(null) }
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Add guide book") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author (optional)") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BOOK_CATEGORIES.forEach { (key, label) ->
                        FilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            label = { Text(label) },
                            enabled = !busy
                        )
                    }
                }

                Text("Subject", style = MaterialTheme.typography.labelLarge)
                SubjectDropdown(subjects, subject, !busy) { subject = it }

                FileOrLinkFields(
                    file = file,
                    link = link,
                    enabled = !busy,
                    onPick = { file = it },
                    onLinkChange = { link = it }
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    error = null
                    viewModel.addGuideBook(title, subject, author, category, file, link) { err ->
                        if (err == null) onDismiss() else error = err
                    }
                }
            ) { Text(if (busy) "Saving…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}