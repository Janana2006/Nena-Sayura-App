package lk.nenasayura.app.ui.teacher.content

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.FilterChipRow
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors

/** My Content tab: everything the teacher uploaded, with open, edit and delete. */
@Composable
fun MyContentTab(
    contents: List<TeacherContent>,
    refreshing: Boolean,
    isFreeTeacher: Boolean,
    onUploadClick: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (TeacherContent) -> Unit,
    onEdit: (TeacherContent, String, String, AccessType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var typeFilter by remember { mutableStateOf<ContentType?>(null) }
    var editing by remember { mutableStateOf<TeacherContent?>(null) }
    var deleting by remember { mutableStateOf<TeacherContent?>(null) }

    val shown = contents.filter { typeFilter == null || it.type == typeFilter }

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(8.dp))
        FilterChipRow(
            label = "Type",
            options = ContentType.entries.toList(),
            selected = typeFilter,
            optionLabel = { it.label },
            onSelected = { typeFilter = it }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${shown.size} item${if (shown.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (refreshing) {
                Box(modifier = Modifier.padding(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
        }

        if (shown.isEmpty()) {
            EmptyView(
                message = if (contents.isEmpty()) {
                    "You haven't uploaded anything yet."
                } else {
                    "No items of this type."
                },
                actionLabel = if (contents.isEmpty()) "Upload material" else null,
                onAction = if (contents.isEmpty()) onUploadClick else null
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.id }) { content ->
                    ContentCard(
                        content = content,
                        onOpen = { FileActions.openFile(context, content.openUrl) },
                        onEdit = { editing = content },
                        onDelete = { deleting = content }
                    )
                }
            }
        }
    }

    editing?.let { item ->
        EditDialog(
            content = item,
            canChooseAccess = !isFreeTeacher,
            onDismiss = { editing = null },
            onSave = { title, description, access ->
                onEdit(item, title, description, access)
                editing = null
            }
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this item?") },
            text = { Text("\"${item.title}\" will be removed and students will no longer see it.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    deleting = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ContentCard(
    content: TeacherContent,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val typePalette = typePalette(content.type)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(typePalette.container, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(typeIcon(content.type), contentDescription = null, tint = typePalette.tint)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = listOf(content.subjectName, formatDate(content.createdAtMillis))
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(text = content.type.label, background = NenaPrimaryContainer, textColor = NenaPrimary)
                if (content.access == AccessType.PAID) {
                    Badge(text = "Paid", background = NenaAccentContainer, textColor = Color(0xFF6B4A00))
                } else {
                    Badge(text = "Free", background = NenaSecondaryContainer, textColor = Color(0xFF004D40))
                }
            }

            if (content.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (content.answerFileUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { FileActions.openFile(context, content.answerFileUrl) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (content.answerFileName.isNotBlank()) "Answer Sheet (${content.answerFileName})" else "Answer Sheet",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpen) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", tint = NenaPrimary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = NenaPrimary)
                }
                IconButton(onClick = onDelete) {
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

@Composable
private fun Badge(text: String, background: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun EditDialog(
    content: TeacherContent,
    canChooseAccess: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, AccessType) -> Unit
) {
    var title by remember { mutableStateOf(content.title) }
    var description by remember { mutableStateOf(content.description) }
    var access by remember { mutableStateOf(content.access) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (canChooseAccess) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccessType.entries.forEach { option ->
                            FilterChip(
                                selected = access == option,
                                onClick = { access = option },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, description, access) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun typeIcon(type: ContentType): ImageVector = when (type) {
    ContentType.SHORT_NOTE -> Icons.Filled.EditNote
    ContentType.TUTE -> Icons.AutoMirrored.Filled.MenuBook
    ContentType.MODEL_PAPER -> Icons.Filled.Quiz
    ContentType.NOTE -> Icons.Filled.Description
    ContentType.VIDEO_LINK -> Icons.Filled.PlayCircle
}

/** Same colours the students see for each kind of material. */
private fun typePalette(type: ContentType): IconPalette = when (type) {
    ContentType.SHORT_NOTE -> NenaIconColors.GuideBooks
    ContentType.TUTE -> NenaIconColors.Papers
    ContentType.MODEL_PAPER -> NenaIconColors.MarkingSchemes
    ContentType.NOTE -> NenaIconColors.ResourceBooks
    ContentType.VIDEO_LINK -> IconPalette(Color(0xFFC62828), Color(0xFFFDE3E3))
}

private fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))