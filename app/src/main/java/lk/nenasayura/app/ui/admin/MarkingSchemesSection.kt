package lk.nenasayura.app.ui.admin

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.MarkingScheme
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.isInCategory
import lk.nenasayura.app.data.model.markingSchemeCategoryTitle
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.util.FileActions

// stored value to the label shown on the chip
private val SCHEME_CATEGORIES = listOf(
    "past" to "Past",
    "model" to "Model",
    "provincial" to "Provincial"
)

/** Marking schemes: Past, Model and Provincial. Add new ones, open or delete the existing ones. */
@Composable
fun MarkingSchemesSection(
    state: AdminUiState,
    schemeVm: MarkingSchemeAdminViewModel = viewModel()
) {
    val ui by schemeVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var category by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<MarkingScheme?>(null) }

    LaunchedEffect(Unit) { schemeVm.loadOnce() }

    val shown = ui.schemes.filter { category == null || it.isInCategory(category!!) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add marking scheme")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null },
                    label = { Text("All (${ui.schemes.size})") }
                )
                SCHEME_CATEGORIES.forEach { (key, label) ->
                    FilterChip(
                        selected = category == key,
                        onClick = { category = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        ui.loadError?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (ui.loading && ui.schemes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        } else if (shown.isEmpty() && ui.loadError == null) {
            item {
                Text(
                    text = "No marking schemes here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(shown, key = { it.id }) { scheme ->
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = scheme.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sub = listOf(
                            state.subjectName(scheme.subjectId),
                            if (scheme.year > 0) scheme.year.toString() else ""
                        ).filter { it.isNotBlank() }.joinToString(" • ")
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Pill(
                            markingSchemeCategoryTitle(scheme.type.ifBlank { "past" }),
                            NenaPrimaryContainer,
                            NenaPrimary
                        )
                    }
                    IconButton(onClick = { FileActions.openFile(context, scheme.fileUrl) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = NenaPrimary)
                    }
                    IconButton(onClick = { deleting = scheme }) {
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

    if (showAdd) {
        AddSchemeDialog(
            streams = state.streams,
            busy = ui.busy,
            schemeVm = schemeVm,
            onDone = {
                Toast.makeText(context, "Marking scheme added", Toast.LENGTH_SHORT).show()
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    deleting?.let { scheme ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this marking scheme?") },
            text = { Text("\"${scheme.title}\" will be removed and students will no longer see it.") },
            confirmButton = {
                TextButton(onClick = {
                    schemeVm.delete(scheme) { err ->
                        Toast.makeText(context, err ?: "Marking scheme deleted", Toast.LENGTH_SHORT).show()
                    }
                    deleting = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddSchemeDialog(
    streams: List<StudyStream>,
    busy: Boolean,
    schemeVm: MarkingSchemeAdminViewModel,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("past") }
    var year by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf<Subject?>(null) }
    var file by remember { mutableStateOf<PickedFile?>(null) }
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Add marking scheme") },
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
                    SCHEME_CATEGORIES.forEach { (key, label) ->
                        FilterChip(
                            selected = type == key,
                            onClick = { type = key },
                            label = { Text(label) },
                            enabled = !busy
                        )
                    }
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

                StreamSubjectPicker(
                    streams = streams,
                    selectedSubject = subject,
                    enabled = !busy,
                    onSubjectSelected = { subject = it }
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
                    schemeVm.add(title, subject, year, type, file, link) { err ->
                        if (err == null) onDone() else error = err
                    }
                }
            ) { Text(if (busy) "Saving…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}