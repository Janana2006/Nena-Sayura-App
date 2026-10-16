package lk.nenasayura.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.student.teachercontent.HeroChip
import lk.nenasayura.app.ui.student.teachercontent.IconBadge
import lk.nenasayura.app.ui.student.teachercontent.IconPill
import lk.nenasayura.app.ui.student.teachercontent.TeacherAvatar
import lk.nenasayura.app.ui.student.teachercontent.style
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.FileActions

private val NewRed = Color(0xFFE53935)
private val NewRedSoft = Color(0xFFFFE3E0)
private val NewRedText = Color(0xFFC62828)
private val AmberText = Color(0xFF6B4A00)
private val TealText = Color(0xFF004D40)

/** One teacher and everything he or she uploaded. */
private class TeacherGroup(
    val teacherId: String,
    val name: String,
    val email: String,
    val uploads: List<TeacherContent>
) {
    val lastUploadMillis: Long get() = uploads.maxOfOrNull { it.createdAtMillis } ?: 0L

    /** "1 Tute • 2 Short Note" */
    val typeSummary: String
        get() = ContentType.entries
            .mapNotNull { type ->
                val count = uploads.count { it.type == type }
                if (count > 0) "$count ${type.label}" else null
            }
            .joinToString(" \u2022 ")

    val subjects: String
        get() = uploads.map { it.subjectName }.filter { it.isNotBlank() }.distinct().joinToString(", ")
}

/**
 * Admin view of teacher uploads.
 * Step 1: the teachers who uploaded something (name, email). A red "N new" badge shows the
 *         uploads the admin has not looked at yet; it goes away after opening the teacher
 *         and comes back when the teacher uploads again.
 * Step 2: tap a teacher to see only that teacher's uploads, and remove any of them.
 * There is no approval step: students see an upload as soon as the teacher saves it.
 */
@Composable
fun TeacherUploadsSection(state: AdminUiState, viewModel: AdminViewModel) {
    val context = LocalContext.current
    UploadSeenStore.init(context)
    val seen by UploadSeenStore.seen.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    // What the admin had already seen when this teacher was opened (to mark the NEW uploads)
    var seenAtOpen by remember { mutableLongStateOf(0L) }
    var typeFilter by remember { mutableStateOf<ContentType?>(null) }
    var removing by remember { mutableStateOf<TeacherContent?>(null) }

    // Group the uploads by teacher; the teacher with the newest upload comes first
    val groups = state.teacherContents
        .groupBy { it.teacherId }
        .map { (id, items) ->
            val user = state.users.firstOrNull { it.uid == id }
            TeacherGroup(
                teacherId = id,
                name = user?.name?.ifBlank { null }
                    ?: items.firstOrNull { it.teacherName.isNotBlank() }?.teacherName
                    ?: "Unknown teacher",
                email = user?.email.orEmpty(),
                uploads = items.sortedByDescending { it.createdAtMillis }
            )
        }
        .sortedByDescending { it.lastUploadMillis }

    val selected = groups.firstOrNull { it.teacherId == selectedId }

    fun closeTeacher() {
        selectedId = null
        typeFilter = null
    }

    // Back goes from a teacher's uploads to the teacher list first
    BackHandler(enabled = selectedId != null) { closeTeacher() }

    if (selectedId == null) {
        // ---------------------------------------------------------- teacher list
        val q = query.trim().lowercase()
        val shown = groups.filter { g ->
            q.isEmpty() ||
                    g.name.lowercase().contains(q) ||
                    g.email.lowercase().contains(q) ||
                    g.subjects.lowercase().contains(q)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "${groups.size} teacher${if (groups.size == 1) "" else "s"} uploaded " +
                            "${state.teacherContents.size} item${if (state.teacherContents.size == 1) "" else "s"}. " +
                            "Tap a teacher to see and manage their uploads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Search teacher name, email or subject") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                )
            }

            if (shown.isEmpty()) {
                item {
                    EmptyView(
                        message = if (groups.isEmpty()) "Teachers have not uploaded anything yet."
                        else "No teachers match your search.",
                        modifier = Modifier.height(160.dp)
                    )
                }
            } else {
                items(shown, key = { it.teacherId }) { group ->
                    val lastSeen = seen[group.teacherId] ?: 0L
                    TeacherRow(
                        group = group,
                        newCount = group.uploads.count { it.createdAtMillis > lastSeen },
                        onClick = {
                            // remember what was new, then mark everything as seen
                            seenAtOpen = lastSeen
                            UploadSeenStore.markSeen(group.teacherId, group.lastUploadMillis)
                            selectedId = group.teacherId
                        }
                    )
                }
            }
        }
    } else {
        // ---------------------------------------------------------- one teacher's uploads
        val uploads = selected?.uploads.orEmpty()
        val shownUploads = uploads.filter { typeFilter == null || it.type == typeFilter }
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NenaPrimaryContainer,
            selectedLabelColor = NenaPrimary
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // gradient header with the teacher's details
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)))
                ) {
                    Column(modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { closeTeacher() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to teachers",
                                    tint = Color.White
                                )
                            }
                            TeacherAvatar(name = selected?.name ?: "T", size = 52, onDark = true)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selected?.name ?: "Teacher",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!selected?.email.isNullOrBlank()) {
                                    Text(
                                        text = selected?.email.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroChip("${uploads.size} upload${if (uploads.size == 1) "" else "s"}")
                            HeroChip("${uploads.count { it.access == AccessType.FREE }} free")
                            HeroChip("${uploads.count { it.access == AccessType.PAID }} paid")
                        }
                    }
                }
            }

            if (uploads.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = typeFilter == null,
                            onClick = { typeFilter = null },
                            label = { Text("All  ${uploads.size}") },
                            colors = chipColors
                        )
                        ContentType.entries
                            .filter { type -> uploads.any { it.type == type } }
                            .forEach { type ->
                                FilterChip(
                                    selected = typeFilter == type,
                                    onClick = { typeFilter = type },
                                    label = { Text("${type.label}  ${uploads.count { it.type == type }}") },
                                    leadingIcon = {
                                        Icon(
                                            type.style().icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = type.style().palette.tint
                                        )
                                    },
                                    colors = chipColors
                                )
                            }
                    }
                }
            }

            if (shownUploads.isEmpty()) {
                item {
                    EmptyView(
                        message = "This teacher has no uploads.",
                        modifier = Modifier.height(160.dp)
                    )
                }
            } else {
                items(shownUploads, key = { it.id }) { content ->
                    UploadCard(
                        content = content,
                        isNew = content.createdAtMillis > seenAtOpen,
                        onOpen = { FileActions.openFile(context, content.openUrl, content.title) },
                        onOpenAnswer = { FileActions.openFile(context, content.answerFileUrl, content.title) },
                        onRemove = { removing = content }
                    )
                }
            }
        }
    }

    removing?.let { item ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove this upload?") },
            text = {
                Text(
                    text = "\"${item.title}\" by ${item.teacherName.ifBlank { "the teacher" }} will be " +
                            "deleted. Students and the teacher will not see it any more. " +
                            "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeContent(item)
                    removing = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } }
        )
    }
}

/** One teacher in the list: name, email and what they uploaded. [newCount] > 0 shows a red badge. */
@Composable
private fun TeacherRow(group: TeacherGroup, newCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // red edge = something new, teal edge = all seen
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (newCount > 0) NewRed else NenaSecondary)
            )
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeacherAvatar(name = group.name, size = 52)
                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (group.email.isNotBlank()) {
                        Text(
                            text = group.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = group.typeSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = NenaPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    if (group.subjects.isNotBlank()) {
                        Text(
                            text = group.subjects,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Last upload: ${formatDate(group.lastUploadMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (newCount > 0) {
                        // only while there is something the admin has not looked at
                        IconPill("$newCount new", NewRedSoft, NewRedText)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadCard(
    content: TeacherContent,
    isNew: Boolean,
    onOpen: () -> Unit,
    onOpenAnswer: () -> Unit,
    onRemove: () -> Unit
) {
    val style = content.type.style()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // edge in the colour of the material type
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(style.palette.tint)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    IconBadge(style.icon, style.palette, size = 46)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = content.title.ifBlank { "(no title)" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOf(content.type.label, content.subjectName)
                                .filter { it.isNotBlank() }
                                .joinToString("  \u2022  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = style.palette.tint
                        )
                        Text(
                            text = formatDate(content.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isNew) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconPill("NEW", NewRedSoft, NewRedText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (content.access == AccessType.PAID) {
                        IconPill("Paid", NenaAccentContainer, AmberText, Icons.Filled.WorkspacePremium)
                    } else {
                        IconPill("Free", NenaSecondaryContainer, TealText)
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

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            Text("Answers")
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = onRemove, shape = RoundedCornerShape(12.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}