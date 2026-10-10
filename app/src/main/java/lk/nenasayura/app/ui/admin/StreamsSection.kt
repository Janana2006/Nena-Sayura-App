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
import androidx.compose.material3.FilterChip
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

// What the admin is doing right now: adding, moving, or confirming a delete
private sealed interface StreamDialog {
    data object AddStream : StreamDialog
    data class AddSubject(val stream: StudyStream, val kanda: Int) : StreamDialog
    data class MoveSubject(val stream: StudyStream, val subject: Subject, val kanda: Int) : StreamDialog
    data class DeleteStream(val stream: StudyStream) : StreamDialog
    data class DeleteSubject(val stream: StudyStream, val subject: Subject) : StreamDialog
}

private fun kandaCountOf(stream: StudyStream): Int = maxOf(3, stream.groups.size)

/**
 * A/L streams (Maths, Bio ...). The subjects of a stream sit in groups 1, 2 and 3.
 * A student picks one subject from each group: a group with one subject is
 * compulsory, a group with two or more lets the student choose one.
 */
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
                    text = "No streams yet. Add a stream, then add its subjects to Group 1, 2 and 3.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.streams, key = { it.id }) { stream ->
            val assignedIds = stream.groups.flatten().map { it.id }.toSet()
            val unassigned = stream.subjects.filter { it.id !in assignedIds }
            val hasKandas = assignedIds.isNotEmpty()

            AdminCard {
                // ---- Stream title ----
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

                // ---- Group 1, 2, 3 ----
                for (index in 0 until kandaCountOf(stream)) {
                    val kandaNumber = index + 1
                    val inKanda = stream.groups.getOrNull(index).orEmpty()

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Group $kandaNumber",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = NenaPrimary
                            )
                            Text(
                                text = when (inKanda.size) {
                                    0 -> "No subjects yet"
                                    1 -> "Compulsory: every student gets this subject"
                                    else -> "The student chooses one of these"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { dialog = StreamDialog.AddSubject(stream, kandaNumber) }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    inKanda.forEach { subject ->
                        SubjectRow(
                            name = subject.name,
                            onMove = { dialog = StreamDialog.MoveSubject(stream, subject, kandaNumber) },
                            onRemove = { dialog = StreamDialog.DeleteSubject(stream, subject) }
                        )
                    }
                }

                // ---- Subjects that are in no group ----
                if (unassigned.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (hasKandas) {
                        Text(
                            text = "Not in any group",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Students cannot pick these. Move each one into a group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No groups set yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Students can pick any 3 of these. Move them into Group 1, 2 and 3 " +
                                    "to make students choose one subject from each group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    unassigned.forEach { subject ->
                        SubjectRow(
                            name = subject.name,
                            onMove = { dialog = StreamDialog.MoveSubject(stream, subject, 1) },
                            onRemove = { dialog = StreamDialog.DeleteSubject(stream, subject) }
                        )
                    }
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

        is StreamDialog.AddSubject -> SubjectKandaDialog(
            title = "Add subject to ${d.stream.name}",
            askName = true,
            kandaCount = kandaCountOf(d.stream),
            initialKanda = d.kanda,
            confirmLabel = "Add",
            busy = state.busy,
            onSave = { name, kanda, done -> viewModel.addSubject(d.stream.id, name, kanda, done) },
            onDismiss = { dialog = null }
        )

        is StreamDialog.MoveSubject -> SubjectKandaDialog(
            title = "Move ${d.subject.name}",
            askName = false,
            kandaCount = kandaCountOf(d.stream),
            initialKanda = d.kanda,
            confirmLabel = "Move",
            busy = state.busy,
            onSave = { _, kanda, done -> viewModel.moveSubject(d.stream.id, d.subject, kanda, done) },
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

/** One subject line: its name, a Move button and a remove (x) button. */
@Composable
private fun SubjectRow(name: String, onMove: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onMove) { Text("Move") }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove subject")
        }
    }
}

/**
 * Add a subject (with its name) or move one (name hidden): choose the group 1, 2 or 3.
 * [onSave] reports back null (done) or an error message.
 */
@Composable
private fun SubjectKandaDialog(
    title: String,
    askName: Boolean,
    kandaCount: Int,
    initialKanda: Int,
    confirmLabel: String,
    busy: Boolean,
    onSave: (String, Int, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kanda by remember { mutableStateOf(initialKanda) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (askName) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject name (for example Physics)") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Group", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (number in 1..kandaCount) {
                        FilterChip(
                            selected = kanda == number,
                            onClick = { kanda = number },
                            label = { Text("Group $number") },
                            enabled = !busy
                        )
                    }
                }
                Text(
                    text = "One subject alone in a group is compulsory. Two or more subjects " +
                            "in a group: the student chooses one of them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onSave(name, kanda) { err -> if (err == null) onDismiss() else error = err }
                }
            ) { Text(if (busy) "Saving…" else confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
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