package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentStatus
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.FileActions

/**
 * Admin review of teacher uploads.
 * The admin opens the file, then approves it (students can see it)
 * or declines it with a message the teacher can read.
 */
@Composable
fun TeacherUploadsSection(state: AdminUiState, viewModel: AdminViewModel) {
    val context = LocalContext.current
    // null = All. Pending first, because that is the admin's to-do list.
    var filter by remember { mutableStateOf<ContentStatus?>(ContentStatus.PENDING) }
    var declining by remember { mutableStateOf<TeacherContent?>(null) }

    val all = state.teacherContents
    val shown = all.filter { filter == null || it.status == filter }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContentStatus.entries.forEach { s ->
                    FilterChip(
                        selected = filter == s,
                        onClick = { filter = s },
                        label = { Text("${s.label} (${all.count { it.status == s }})") }
                    )
                }
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("All (${all.size})") }
                )
            }
        }

        if (shown.isEmpty()) {
            item {
                EmptyView(
                    message = when (filter) {
                        ContentStatus.PENDING -> "No uploads are waiting for review."
                        ContentStatus.APPROVED -> "No approved uploads yet."
                        ContentStatus.DECLINED -> "No declined uploads."
                        null -> "Teachers have not uploaded anything yet."
                    },
                    modifier = Modifier.height(160.dp)
                )
            }
        } else {
            items(shown, key = { it.id }) { content ->
                UploadReviewCard(
                    content = content,
                    onOpen = { FileActions.openFile(context, content.openUrl, content.title) },
                    onOpenAnswer = { FileActions.openFile(context, content.answerFileUrl, content.title) },
                    onApprove = { viewModel.approveContent(content) },
                    onDecline = { declining = content }
                )
            }
        }
    }

    declining?.let { item ->
        DeclineDialog(
            content = item,
            onDismiss = { declining = null },
            onSend = { reason, onError ->
                viewModel.declineContent(item, reason) { error ->
                    if (error == null) declining = null else onError(error)
                }
            }
        )
    }
}

@Composable
private fun UploadReviewCard(
    content: TeacherContent,
    onOpen: () -> Unit,
    onOpenAnswer: () -> Unit,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    AdminCard {
        Text(
            text = content.title.ifBlank { "(no title)" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val sub = listOf(
            content.teacherName.ifBlank { "Unknown teacher" },
            content.subjectName,
            formatDate(content.createdAtMillis)
        ).filter { it.isNotBlank() }.joinToString(" • ")
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(content.type.label, NenaPrimaryContainer, NenaPrimary)
            Pill(
                if (content.access == AccessType.PAID) "Paid" else "Free",
                NenaAccentContainer,
                Color(0xFF6B4A00)
            )
            when (content.status) {
                ContentStatus.PENDING -> Pill("Pending", NenaAccentContainer, Color(0xFF6B4A00))
                ContentStatus.APPROVED -> Pill("Approved", NenaSecondaryContainer, Color(0xFF004D40))
                ContentStatus.DECLINED -> Pill("Declined", Color(0xFFFFDAD6), Color(0xFF93000A))
            }
        }

        if (content.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }

        // The admin must be able to look at the material before deciding
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (content.openUrl.isNotBlank()) {
                OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (content.openUrl == content.videoUrl && content.videoUrl.isNotBlank()) "Open video" else "View file")
                }
            }
            if (content.answerFileUrl.isNotBlank()) {
                OutlinedButton(onClick = onOpenAnswer, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Answer sheet")
                }
            }
        }

        if (content.status == ContentStatus.DECLINED) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFDAD6),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Message sent to the teacher",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93000A)
                    )
                    Text(
                        text = content.declineReason.ifBlank { "No reason was given." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF410002)
                    )
                }
            }
        }

        // Approve / Decline. A decision can be changed later.
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (content.status != ContentStatus.DECLINED) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (content.status == ContentStatus.APPROVED) "Take down" else "Decline")
                }
            }
            if (content.status != ContentStatus.APPROVED) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve")
                }
            }
        }
    }
}

/** Asks the admin for the message the teacher will read. */
@Composable
private fun DeclineDialog(
    content: TeacherContent,
    onDismiss: () -> Unit,
    onSend: (reason: String, onError: (String) -> Unit) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decline this upload?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "\"${content.title}\" by ${content.teacherName.ifBlank { "the teacher" }}. " +
                            "Students will not see it. Write a message so the teacher knows what to fix.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        error = null
                    },
                    label = { Text("Reason for the teacher") },
                    minLines = 3,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(reason) { message -> error = message } }) {
                Text("Decline", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}