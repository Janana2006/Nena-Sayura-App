package lk.nenasayura.app.ui.teacher.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentStatus
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaSecondary

private val FILE_TYPES = arrayOf(
    "application/pdf",
    "image/*",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
)

/** Upload tab: pick the type, subject and file (or video link), choose Free or Paid. */
@Composable
fun UploadScreen(
    profile: TeacherProfile,
    onUploaded: (ContentStatus) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UploadViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(profile.subjectIds) { viewModel.setSubjects(profile.subjects) }

    // After a successful upload: tell the Home screen once, then reset the flag.
    LaunchedEffect(state.uploadedOk) {
        if (state.uploadedOk) {
            onUploaded(state.uploadedStatus)
            viewModel.clearUploaded()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val (name, size) = context.queryFileInfo(uri)
            viewModel.onFilePicked(uri, name, size)
        }
    }

    val answerFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val (name, size) = context.queryFileInfo(uri)
            viewModel.onAnswerFilePicked(uri, name, size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 1) Type
        Label("What are you uploading?")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ContentType.entries) { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { viewModel.onTypeChange(type) },
                    label = { Text(type.label) },
                    enabled = !state.isUploading
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2) Title
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Title") },
            singleLine = true,
            enabled = !state.isUploading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3) Subject (one subject: shown as text, several: a dropdown)
        Label("Subject")
        if (state.subjects.size <= 1) {
            Text(
                text = state.subjects.firstOrNull()?.name
                    ?: "No subject found. Add one in the Profile tab.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.subjects.isEmpty()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        } else {
            SubjectPicker(
                subjects = state.subjects,
                selected = state.selectedSubject,
                enabled = !state.isUploading,
                onSelect = viewModel::onSubjectSelected
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4) Description
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description") },
            minLines = 3,
            enabled = !state.isUploading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5) File or video link
        if (state.type == ContentType.VIDEO_LINK) {
            OutlinedTextField(
                value = state.videoUrl,
                onValueChange = viewModel::onVideoUrlChange,
                label = { Text("Video link (YouTube, Drive...)") },
                placeholder = { Text("https://") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                enabled = !state.isUploading,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Label("File")
            OutlinedButton(
                onClick = { filePicker.launch(FILE_TYPES) },
                enabled = !state.isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.fileName.ifBlank { "Choose file (PDF, image, Word, PowerPoint)" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (state.type == ContentType.MODEL_PAPER) {
                Spacer(modifier = Modifier.height(16.dp))
                Label("Answer Sheet (optional)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { answerFilePicker.launch(FILE_TYPES) },
                        enabled = !state.isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.answerFileName.ifBlank { "Choose answer sheet (PDF, image, Word, PowerPoint)" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.answerFileUri != null && !state.isUploading) {
                        IconButton(onClick = { viewModel.clearAnswerFile() }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Remove answer sheet",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6) Free / Paid (a free teacher's uploads are always free)
        Label("Who can open this?")
        if (profile.isFree) {
            Text(
                text = "You are a free teacher, so every upload is free for all students. " +
                        "Change your plan in the Profile tab to offer paid material.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccessOption(
                    label = "Free",
                    description = "All students can open it",
                    icon = Icons.Filled.LockOpen,
                    color = NenaSecondary,
                    selected = state.access == AccessType.FREE,
                    onClick = { viewModel.onAccessChange(AccessType.FREE) },
                    modifier = Modifier.weight(1f)
                )
                AccessOption(
                    label = "Paid",
                    description = "Only students who paid you can open it",
                    icon = Icons.Filled.Lock,
                    color = NenaAccent,
                    selected = state.access == AccessType.PAID,
                    onClick = { viewModel.onAccessChange(AccessType.PAID) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 7) Upload button
        Button(
            onClick = { viewModel.submit(profile.uid, profile.name, profile.isFree) },
            enabled = !state.isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Uploading…")
            } else {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SubjectPicker(
    subjects: List<Subject>,
    selected: Subject?,
    enabled: Boolean,
    onSelect: (Subject) -> Unit
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
                    text = selected?.name ?: "Select subject",
                    color = if (selected != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject.name) },
                    onClick = {
                        onSelect(subject)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AccessOption(
    label: String,
    description: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Reads the file name and size (bytes) of a picked document. */
private fun Context.queryFileInfo(uri: Uri): Pair<String, Long> {
    var name = "file"
    var size = -1L
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return name to size
}