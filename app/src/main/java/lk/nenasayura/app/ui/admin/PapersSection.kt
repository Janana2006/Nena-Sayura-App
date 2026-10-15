package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.PROVINCES
import lk.nenasayura.app.data.model.Paper
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.isInCategory
import lk.nenasayura.app.data.model.paperTypeLabel
import lk.nenasayura.app.data.model.termLabel
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.util.FileActions

// stored value to the label shown on the chip
private val PAPER_CATEGORIES = listOf(
    "past" to "Past",
    "model" to "Model",
    "provincial" to "Provincial"
)

/** Papers: Past, Model and Provincial. Add new ones, open or delete the existing ones. */
@Composable
fun PapersSection(state: AdminUiState, viewModel: AdminViewModel) {
    val context = LocalContext.current
    var category by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Paper?>(null) }

    val shown = state.papers.filter { category == null || it.isInCategory(category!!) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add paper")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("All (${state.papers.size})") }
                )
                PAPER_CATEGORIES.forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item { EmptyView(message = "No papers here yet.", modifier = Modifier.height(160.dp)) }
        } else {
            items(shown, key = { it.id }) { paper ->
                AdminCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = paper.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = listOf(
                                state.subjectName(paper.subjectId),
                                if (paper.year > 0) paper.year.toString() else "",
                                paper.province,
                                termLabel(paper.term)
                            ).filter { it.isNotBlank() }.joinToString(" • ")
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Pill(
                                paperTypeLabel(paper.type.ifBlank { "past" }),
                                NenaPrimaryContainer,
                                NenaPrimary
                            )
                        }
                        IconButton(onClick = { FileActions.openFile(context, paper.fileUrl) }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = NenaPrimary)
                        }
                        IconButton(onClick = { deleting = paper }) {
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
        AddPaperDialog(
            streams = state.streams,
            busy = state.busy,
            viewModel = viewModel,
            onDismiss = { showAdd = false }
        )
    }

    deleting?.let { paper ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this paper?") },
            text = { Text("\"${paper.title}\" will be removed and students will no longer see it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePaper(paper)
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddPaperDialog(
    streams: List<lk.nenasayura.app.data.model.StudyStream>,
    busy: Boolean,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("past") }
    var term by remember { mutableStateOf(0) } // 0 = not chosen, 1-3 = term
    var province by remember { mutableStateOf("") } // "" = not chosen
    var year by remember { mutableStateOf("") }
    var selectedStream by remember { mutableStateOf<lk.nenasayura.app.data.model.StudyStream?>(null) }
    var subject by remember { mutableStateOf<Subject?>(null) }
    var file by remember { mutableStateOf<PickedFile?>(null) }
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val availableSubjects = selectedStream?.subjects ?: emptyList()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Add paper") },
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

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PAPER_CATEGORIES.forEach { (key, label) ->
                        FilterChip(
                            selected = type == key,
                            onClick = {
                                type = key
                                if (key != "provincial") {
                                    term = 0
                                    province = ""
                                }
                            },
                            label = { Text(label) },
                            enabled = !busy
                        )
                    }
                }

                // Province and Term are only needed for Provincial papers
                if (type == "provincial") {
                    Text("Province", style = MaterialTheme.typography.labelLarge)
                    ProvinceDropdown(
                        selected = province,
                        enabled = !busy,
                        onSelect = { province = it }
                    )

                    Text("Term", style = MaterialTheme.typography.labelLarge)
                    TermDropdown(
                        selected = term,
                        enabled = !busy,
                        onSelect = { term = it }
                    )
                }

                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Year") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Stream", style = MaterialTheme.typography.labelLarge)
                StreamDropdown(
                    streams = streams,
                    selected = selectedStream,
                    enabled = !busy,
                    onSelect = { newStream ->
                        selectedStream = newStream
                        if (subject != null && !newStream.subjects.any { it.id == subject?.id }) {
                            subject = null
                        }
                    }
                )

                Text("Subject", style = MaterialTheme.typography.labelLarge)
                SubjectDropdown(
                    subjects = availableSubjects,
                    selected = subject,
                    enabled = !busy && selectedStream != null,
                    placeholder = if (selectedStream == null) "Select a stream first" else "Select subject",
                    onSelect = { subject = it }
                )

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
                    if (type == "provincial" && province.isBlank()) {
                        error = "Please select a province"
                    } else if (type == "provincial" && term == 0) {
                        error = "Please select a term"
                    } else if (selectedStream == null) {
                        error = "Please select a stream"
                    } else if (subject == null) {
                        error = "Please select a subject"
                    } else {
                        viewModel.addPaper(title, subject, year, type, term, province, file, link) { err ->
                            if (err == null) onDismiss() else error = err
                        }
                    }
                }
            ) { Text(if (busy) "Saving…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

/** Drop-down used to choose the term (1st, 2nd or 3rd) of a Provincial paper. */
@Composable
private fun TermDropdown(
    selected: Int,          // 0 = nothing chosen yet
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selected in 1..3) termLabel(selected) else "Select term",
                    color = if (selected in 1..3) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..3).forEach { t ->
                DropdownMenuItem(
                    text = { Text(termLabel(t)) },
                    onClick = {
                        onSelect(t)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Drop-down used to choose the province of a Provincial paper. */
@Composable
private fun ProvinceDropdown(
    selected: String,       // "" = nothing chosen yet
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selected.isNotBlank()) selected else "Select province",
                    color = if (selected.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PROVINCES.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(name)
                        expanded = false
                    }
                )
            }
        }
    }
}