package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.ui.theme.NenaPrimary

// What the admin is doing right now: adding, or confirming a delete
private sealed interface StreamDialog {
    data object AddStream : StreamDialog
    data class AddSubject(val stream: StudyStream) : StreamDialog
    data class DeleteStream(val stream: StudyStream) : StreamDialog
    data class DeleteSubject(val stream: StudyStream, val subject: Subject) : StreamDialog
}

/** A/L streams (Maths, Bio ...) and the subjects inside each one. */
@Composable
fun StreamsSection(state: AdminUiState, viewModel: AdminViewModel) {
    var dialog by remember { mutableStateOf<StreamDialog?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { dialog = StreamDialog.AddStream }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add stream")
            }
        }

        if (state.streams.isEmpty()) {
            item {
                Text(
                    text = "No streams yet. Add a stream, then add its subjects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.streams, key = { it.id }) { stream ->
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stream.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${stream.subjects.size} subject${if (stream.subjects.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { dialog = StreamDialog.DeleteStream(stream) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete stream",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (stream.subjects.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                stream.subjects.forEach { subject ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { dialog = StreamDialog.DeleteSubject(stream, subject) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove subject")
                        }
                    }
                }

                TextButton(onClick = { dialog = StreamDialog.AddSubject(stream) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add subject", color = NenaPrimary)
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit

        StreamDialog.AddStream -> NameDialog(
            title = "Add stream",
            label = "Stream name (for example Maths Stream)",
            busy = state.busy,
            onSave = { name, done -> viewModel.addStream(name, done) },
            onDismiss = { dialog = null }
        )

        is StreamDialog.AddSubject -> NameDialog(
            title = "Add subject to ${d.stream.name}",
            label = "Subject name (for example Physics)",
            busy = state.busy,
            onSave = { name, done -> viewModel.addSubject(d.stream.id, name, done) },
            onDismiss = { dialog = null }
        )

        is StreamDialog.DeleteStream -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete ${d.stream.name}?") },
            text = { Text("The stream and its subject list will be removed. Papers and books already added keep their subject link.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteStream(d.stream)
                    dialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )

        is StreamDialog.DeleteSubject -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Remove ${d.subject.name}?") },
            text = { Text("It will be removed from ${d.stream.name}.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSubject(d.stream.id, d.subject)
                    dialog = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )
    }
}

/** A small dialog with one text field. [onSave] reports back null (done) or an error message. */
@Composable
private fun NameDialog(
    title: String,
    label: String,
    busy: Boolean,
    onSave: (String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(label) },
                    singleLine = true,
                    enabled = !busy,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    error = null
                    onSave(name) { err -> if (err == null) onDismiss() else error = err }
                }
            ) { Text(if (busy) "Saving…" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}