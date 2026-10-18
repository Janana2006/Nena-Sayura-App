package lk.nenasayura.app.ui.student.teachercontent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.AccessType
import lk.nenasayura.app.data.model.ContentType
import lk.nenasayura.app.data.model.TeacherContent
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.DownloadStore
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.util.YouTubeLinks

private val AmberText = Color(0xFF6B4A00)
private val TealText = Color(0xFF004D40)
private val LockedGrey = Color(0xFFB0B6C3)

/**
 * Student screen: one teacher's notes, tutes, papers and videos (live as soon as the teacher uploads).
 * A paid teacher shows the fee and an Unlock button until the student has paid.
 */
@Composable
fun TeacherMaterialsScreen(
    teacherId: String,
    onBack: () -> Unit,
    onPay: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    viewModel: TeacherMaterialsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // the video the student is watching right now (null = player closed)
    var playing by remember { mutableStateOf<TeacherContent?>(null) }

    LaunchedEffect(teacherId) { viewModel.load(teacherId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        val teacher = state.teacher

        when {
            state.loading -> Column(modifier = modifier) {
                ScreenHeader(title = "Teacher", onBack = onBack)
                LoadingView(modifier = Modifier.weight(1f))
            }

            state.errorMessage != null || teacher == null -> Column(modifier = modifier) {
                ScreenHeader(title = "Teacher", onBack = onBack)
                ErrorView(
                    message = state.errorMessage ?: "Teacher not found.",
                    onRetry = { viewModel.load(teacherId) },
                    modifier = Modifier.weight(1f)
                )
            }

            else -> LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ---- gradient header with the teacher's details
                item(key = "hero") {
                    GradientHero(
                        onBack = onBack,
                        bottomPadding = 48.dp,
                        actions = {
                            // Opens the Downloads screen (files saved in the app, work offline)
                            IconButton(onClick = onOpenDownloads) {
                                Icon(Icons.Filled.DownloadDone, contentDescription = "Downloads", tint = Color.White)
                            }
                            IconButton(onClick = { viewModel.load(teacherId) }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TeacherAvatar(name = teacher.name, size = 68, onDark = true)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = teacher.name.ifBlank { "Teacher" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (teacher.streamName.isNotBlank()) {
                                    Text(
                                        text = teacher.streamName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }
                        }
                        if (teacher.subjectNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HeroChipRow(teacher.subjectNames)
                        }
                        if (teacher.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = teacher.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ---- floating card: fee + unlock button (or "unlocked" / "free")
                item(key = "access") {
                    AccessCard(
                        teacher = teacher,
                        hasPaid = state.hasPaid,
                        onPay = onPay,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .pullUp(30.dp)
                    )
                }

                // ---- materials title + type filter
                item(key = "section") {
                    Column(modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Materials",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(NenaPrimaryContainer, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${state.items.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NenaPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (state.items.isNotEmpty()) {
                            TypeChips(
                                materials = state.items,
                                selected = state.typeFilter,
                                onSelected = viewModel::onTypeFilter
                            )
                        }
                    }
                }

                val shown = state.shown
                if (shown.isEmpty()) {
                    item(key = "empty") { EmptyMaterials() }
                } else {
                    items(shown, key = { it.id }) { item ->
                        MaterialCard(
                            item = item,
                            unlocked = state.isUnlocked(item),
                            feeLabel = formatFee(teacher.fee),
                            onPay = onPay,
                            onOpen = {
                                // a YouTube link plays inside the app; anything else opens as before
                                val isYoutube = item.type == ContentType.VIDEO_LINK &&
                                        YouTubeLinks.extractId(item.openUrl) != null
                                if (isYoutube) {
                                    playing = item
                                } else {
                                    FileActions.openFile(context, item.openUrl, item.title)
                                }
                            },
                            onOpenAnswer = {
                                FileActions.openFile(context, item.answerFileUrl, item.title)
                            },
                            onOpenDownloads = onOpenDownloads,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    playing?.let { video ->
        VideoPlayerDialog(
            title = video.title,
            description = video.description,
            videoUrl = video.openUrl,
            onClose = { playing = null }
        )
    }
}

// ------------------------------------------------------------ access card

@Composable
private fun AccessCard(
    teacher: TeacherProfile,
    hasPaid: Boolean,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        when {
            // Paid teacher, not paid yet
            teacher.isPaidTeacher && !hasPaid -> Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "COURSE FEE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFee(teacher.fee),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = NenaPrimary
                    )
                    Text(
                        text = "One payment unlocks every paid material",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onPay,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NenaAccent,
                        contentColor = Color(0xFF3B2A00)
                    )
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlock", fontWeight = FontWeight.Bold)
                }
            }

            // Paid teacher, already paid
            teacher.isPaidTeacher -> StatusRow(
                icon = {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                },
                iconBackground = Color(0xFFDFF3E1),
                title = "Unlocked",
                subtitle = "You have full access to this teacher's materials"
            )

            // Free teacher
            else -> StatusRow(
                icon = {
                    Icon(Icons.Filled.CardGiftcard, null, tint = Color(0xFF00796B), modifier = Modifier.size(24.dp))
                },
                iconBackground = NenaSecondaryContainer,
                title = "Free teacher",
                subtitle = "All materials are open for you"
            )
        }
    }
}

@Composable
private fun StatusRow(
    icon: @Composable () -> Unit,
    iconBackground: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------------ filter chips

@Composable
private fun TypeChips(
    materials: List<TeacherContent>,
    selected: ContentType?,
    onSelected: (ContentType?) -> Unit
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = NenaPrimaryContainer,
        selectedLabelColor = NenaPrimary
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text("All  ${materials.size}") },
                colors = chipColors
            )
        }
        // only the kinds this teacher really has
        items(ContentType.entries.filter { type -> materials.any { it.type == type } }) { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text("${type.label}  ${materials.count { it.type == type }}") },
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

@Composable
private fun EmptyMaterials() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(NenaPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.FolderOpen, null, tint = NenaPrimary, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This teacher has not uploaded any materials in this category.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ------------------------------------------------------------ material card

@Composable
private fun MaterialCard(
    item: TeacherContent,
    unlocked: Boolean,
    feeLabel: String,
    onPay: () -> Unit,
    onOpen: () -> Unit,
    onOpenAnswer: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = item.type.style()

    // Download state: saved inside the app (Downloads screen) / saving right now
    val context = LocalContext.current
    remember { DownloadStore.load(context) }
    val savedItems by DownloadStore.items.collectAsStateWithLifecycle()
    val busyIds by DownloadStore.busy.collectAsStateWithLifecycle()
    val isSaved = savedItems.any { it.url == item.fileUrl }
    val isSaving = DownloadStore.idFor(item.fileUrl) in busyIds

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // coloured edge: the colour of the material type, grey when locked
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (unlocked) style.palette.tint else LockedGrey)
            )

            Column(modifier = Modifier
                .weight(1f)
                .padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (unlocked) {
                        IconBadge(style.icon, style.palette, size = 48)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFEDEFF4), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = Color(0xFF8A91A2), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOf(item.type.label, item.subjectName)
                                .filter { it.isNotBlank() }
                                .joinToString("  \u2022  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = style.palette.tint
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.access == AccessType.PAID) {
                        IconPill("Paid", NenaAccentContainer, AmberText, Icons.Filled.WorkspacePremium)
                    } else {
                        IconPill("Free", NenaSecondaryContainer, TealText)
                    }
                }

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (unlocked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isVideo = item.type == ContentType.VIDEO_LINK
                        Button(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                            Icon(
                                if (isVideo) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isVideo) "Watch" else "Open")
                        }
                        // Download: same as the past papers (saved in the app, works offline).
                        // Videos can't be downloaded.
                        if (!isVideo && item.fileUrl.isNotBlank()) {
                            val showLabel = item.answerFileUrl.isBlank() // keep the row short when there is an Answers button
                            FilledTonalButton(
                                onClick = {
                                    // Already saved: go to the Downloads screen to see it
                                    if (isSaved) onOpenDownloads()
                                    else FileActions.downloadFile(context, item.fileUrl, item.title)
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = if (isSaved) "Saved in the app" else "Download",
                                    modifier = Modifier.size(18.dp)
                                )
                                if (showLabel) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        when {
                                            isSaved -> "Saved"
                                            isSaving -> "Saving..."
                                            else -> "Download"
                                        }
                                    )
                                }
                            }
                        }
                        if (item.answerFileUrl.isNotBlank()) {
                            OutlinedButton(onClick = onOpenAnswer, shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Answers")
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Locked. Pay to open.",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onPay,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NenaAccent,
                                contentColor = Color(0xFF3B2A00)
                            )
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (feeLabel.isNotBlank()) "Unlock  $feeLabel" else "Unlock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}