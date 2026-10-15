package lk.nenasayura.app.ui.student.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.viewmodel.SetupViewModel

/**
 * Lets a student change the stream and subjects after the first setup.
 * Uses the same rules as the first setup (compulsory subjects, slots, or any 3 subjects).
 */
@Composable
fun EditStudyScreen(
    currentStreamId: String,
    currentSubjectIds: List<String>,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Start a fresh setup for a student
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.start("student")
        started = true
    }

    // When the streams are loaded, tick the student's current stream and subjects
    var preselected by remember { mutableStateOf(false) }
    LaunchedEffect(started, state.loading, state.streams) {
        if (started && !preselected && !state.loading && state.streams.isNotEmpty()) {
            preselected = true
            if (state.streams.any { it.id == currentStreamId }) {
                viewModel.selectStream(currentStreamId)
                currentSubjectIds.forEach { viewModel.toggleSubject(it) }
            }
        }
    }

    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.consumeDone()
            onSaved()
        }
    }

    val error = state.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stream & subjects",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose the stream and subjects you study",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }

        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.streams.isEmpty() -> Text(
                text = error ?: "No streams are available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> {
                SectionLabel("Stream")
                state.streams.forEach { stream ->
                    SelectableRow(
                        title = stream.name,
                        subtitle = "${stream.subjects.size} subjects",
                        selected = stream.id == state.selectedStreamId,
                        onClick = { viewModel.selectStream(stream.id) }
                    )
                }

                val selectedStream = state.selectedStream
                if (selectedStream != null) {
                    if (state.hasGroups) {
                        // Slots: Subject 1, 2, 3
                        selectedStream.groups.forEachIndexed { index, group ->
                            SectionLabel("Subject ${index + 1}")
                            group.forEach { subject ->
                                SelectableRow(
                                    title = subject.name,
                                    subtitle = if (group.size == 1) "Compulsory" else null,
                                    selected = subject.id in state.selectedSubjectIds,
                                    onClick = { viewModel.toggleSubject(subject.id) }
                                )
                            }
                        }
                    } else {
                        SectionLabel("Subjects (${state.selectedSubjectIds.size}/${state.requiredCount})")
                        selectedStream.subjects.forEach { subject ->
                            SelectableRow(
                                title = subject.name,
                                subtitle = null,
                                selected = subject.id in state.selectedSubjectIds,
                                onClick = { viewModel.toggleSubject(subject.id) }
                            )
                        }
                    }
                }

                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { viewModel.confirm() },
                    enabled = state.canConfirm && !state.saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Save changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) NenaPrimaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NenaPrimary
                )
            }
        }
    }
}