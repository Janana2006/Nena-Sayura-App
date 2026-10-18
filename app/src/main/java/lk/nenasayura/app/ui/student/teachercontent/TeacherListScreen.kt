package lk.nenasayura.app.ui.student.teachercontent

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.browse.PaperBrowseState
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

private val AmberText = Color(0xFF6B4A00)
private val TealText = Color(0xFF004D40)

// Every subject has its own colour (same colour in the filter strip and on the teacher cards)
private val SubjectPalette = listOf(
    Color(0xFF2F6FED), Color(0xFF1E9E5A), Color(0xFFF28C00),
    Color(0xFF8E5CF5), Color(0xFFE5484D), Color(0xFF0BA5C4)
)

private fun subjectColor(name: String): Color =
    SubjectPalette[(name.trim().lowercase().hashCode() and 0x7fffffff) % SubjectPalette.size]

/**
 * Student screen "My teacher".
 * By default it lists only the teachers of the subjects the student chose at sign-up.
 * The student can pick one of their subjects, choose "All teachers", search any teacher or
 * subject, or change the subjects. Free teachers are listed first, paid teachers below.
 */
@Composable
fun TeacherListScreen(
    onBack: () -> Unit,
    onOpenTeacher: (String) -> Unit,
    /** The stream + subjects saved in the student's profile. Empty = show every teacher. */
    selection: PaperBrowseState = PaperBrowseState(),
    /** Opens the "change stream / subjects" screen. null = no Change button. */
    onChangeSelection: (() -> Unit)? = null,
    /** Opens the Downloads screen (files saved in the app, work offline). */
    onOpenDownloads: () -> Unit = {},
    viewModel: TeacherListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    // false = only the teachers of my subjects, true = every teacher
    var showAll by remember(selection.hasSelection) { mutableStateOf(!selection.hasSelection) }
    // one of my subjects picked on the chips (null = all my subjects)
    var subjectFilter by remember { mutableStateOf<Subject?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            GradientHero(
                onBack = onBack,
                bottomPadding = 22.dp,
                actions = {
                    IconButton(onClick = onOpenDownloads) {
                        Icon(Icons.Filled.DownloadDone, contentDescription = "Downloads", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            ) {
                Text(
                    text = "My teacher",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (selection.hasSelection) "Teachers for your subjects"
                    else "Pick a teacher and start learning",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                SearchField(query = query, onChange = { query = it })
            }

            when {
                state.loading -> LoadingView(modifier = Modifier.weight(1f))

                state.errorMessage != null -> ErrorView(
                    message = state.errorMessage ?: "",
                    onRetry = { viewModel.load() },
                    modifier = Modifier.weight(1f)
                )

                state.teachers.isEmpty() -> EmptyView(
                    message = "No teacher has uploaded any materials yet.",
                    modifier = Modifier.weight(1f)
                )

                else -> {
                    val q = query.trim().lowercase()
                    val chosen = subjectFilter

                    fun matchesQuery(t: TeacherProfile): Boolean =
                        t.name.lowercase().contains(q) ||
                                t.streamName.lowercase().contains(q) ||
                                t.subjectNames.any { it.lowercase().contains(q) }

                    fun teaches(t: TeacherProfile, subject: Subject): Boolean =
                        subject.id in t.subjectIds ||
                                t.subjectNames.any { it.equals(subject.name, ignoreCase = true) }

                    // Searching looks at EVERY teacher, so a student can find a teacher of another subject
                    fun visible(t: TeacherProfile): Boolean = when {
                        q.isNotEmpty() -> matchesQuery(t)
                        showAll || !selection.hasSelection -> true
                        chosen != null -> teaches(t, chosen)
                        else -> selection.subjects.any { teaches(t, it) }
                    }

                    if (selection.hasSelection) {
                        if (q.isEmpty()) {
                            SubjectFilterStrip(
                                selection = selection,
                                showAll = showAll,
                                chosen = chosen,
                                onMine = { showAll = false; subjectFilter = null },
                                onSubject = { showAll = false; subjectFilter = it },
                                onAll = { showAll = true; subjectFilter = null },
                                onChange = onChangeSelection
                            )
                        } else {
                            Text(
                                text = "Searching all teachers",
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val free = state.freeTeachers.filter { visible(it) }
                    val paid = state.paidTeachers.filter { visible(it) }

                    if (free.isEmpty() && paid.isEmpty()) {
                        NoTeachersFound(
                            message = if (q.isNotEmpty()) "No teachers match \"$query\"."
                            else "No teachers with materials for your subjects yet.",
                            canShowAll = q.isEmpty() && !showAll,
                            onShowAll = { showAll = true; subjectFilter = null },
                            onChange = onChangeSelection,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (free.isNotEmpty()) {
                                item(key = "header_free") {
                                    SectionHeader(
                                        title = "Free teachers",
                                        count = free.size,
                                        icon = { Icon(Icons.Filled.CardGiftcard, null, tint = Color(0xFF00796B), modifier = Modifier.size(18.dp)) },
                                        iconBackground = NenaSecondaryContainer
                                    )
                                }
                                items(free, key = { "free_" + it.uid }) { teacher ->
                                    TeacherCard(
                                        teacher = teacher,
                                        materialCount = state.materialCounts[teacher.uid] ?: 0,
                                        alreadyPaid = false,
                                        onClick = { onOpenTeacher(teacher.uid) }
                                    )
                                }
                            }
                            if (paid.isNotEmpty()) {
                                item(key = "header_paid") {
                                    SectionHeader(
                                        title = "Paid teachers",
                                        count = paid.size,
                                        icon = { Icon(Icons.Filled.WorkspacePremium, null, tint = Color(0xFFB07A00), modifier = Modifier.size(18.dp)) },
                                        iconBackground = NenaAccentContainer
                                    )
                                }
                                items(paid, key = { "paid_" + it.uid }) { teacher ->
                                    TeacherCard(
                                        teacher = teacher,
                                        materialCount = state.materialCounts[teacher.uid] ?: 0,
                                        alreadyPaid = teacher.uid in state.paidTeacherIds,
                                        onClick = { onOpenTeacher(teacher.uid) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "My subjects" | one chip per subject | "All teachers", and the Change button. */
@Composable
private fun SubjectFilterStrip(
    selection: PaperBrowseState,
    showAll: Boolean,
    chosen: Subject?,
    onMine: () -> Unit,
    onSubject: (Subject) -> Unit,
    onAll: () -> Unit,
    onChange: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SubjectPill(
                    label = "My subjects",
                    selected = !showAll && chosen == null,
                    color = NenaPrimary,
                    onClick = onMine
                )
            }
            items(selection.subjects, key = { it.id }) { subject ->
                SubjectPill(
                    label = subject.name,
                    selected = !showAll && chosen?.id == subject.id,
                    color = subjectColor(subject.name),
                    onClick = { onSubject(subject) }
                )
            }
            item {
                SubjectPill(
                    label = "All teachers",
                    selected = showAll,
                    color = NenaSecondary,
                    onClick = onAll
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    showAll -> "Showing all teachers"
                    selection.streamName.isNotBlank() -> "Your stream: ${selection.streamName}"
                    else -> "Showing teachers for your subjects"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onChange != null) {
                TextButton(onClick = onChange) { Text("Change subjects") }
            }
        }
    }
}

/** A rounded subject filter: filled with its colour when selected, soft tint when not. */
@Composable
private fun SubjectPill(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else color,
            maxLines = 1
        )
    }
}

@Composable
private fun NoTeachersFound(
    message: String,
    canShowAll: Boolean,
    onShowAll: () -> Unit,
    onChange: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (canShowAll) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onShowAll, shape = RoundedCornerShape(14.dp)) { Text("Show all teachers") }
        }
        if (onChange != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onChange, shape = RoundedCornerShape(14.dp)) { Text("Change subjects") }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Search any teacher or subject") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: @Composable () -> Unit,
    iconBackground: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(iconBackground.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E2433)
        )
        Box(
            modifier = Modifier
                .background(Color.White, CircleShape)
                .padding(horizontal = 11.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E2433)
            )
        }
    }
}

@Composable
private fun TeacherCard(
    teacher: TeacherProfile,
    materialCount: Int,
    alreadyPaid: Boolean,
    onClick: () -> Unit
) {
    val isPaid = teacher.isPaidTeacher
    val accent = if (isPaid) NenaAccent else NenaSecondary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // colour band on top: teal = free, gold = paid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.3f))))
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // avatar with a gradient ring
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(Brush.linearGradient(listOf(accent, NenaPrimary)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(61.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            TeacherAvatar(name = teacher.name, size = 54)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = teacher.name.ifBlank { "Teacher" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (teacher.streamName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(NenaPrimaryContainer, RoundedCornerShape(50))
                                    .padding(horizontal = 9.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = teacher.streamName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NenaPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    if (isPaid) {
                        if (alreadyPaid) {
                            IconPill("Unlocked", NenaSecondaryContainer, TealText, Icons.Filled.CheckCircle)
                        } else {
                            IconPill(formatFee(teacher.fee), NenaAccentContainer, AmberText, Icons.Filled.WorkspacePremium)
                        }
                    } else {
                        IconPill("Free", NenaSecondaryContainer, TealText, Icons.Filled.CardGiftcard)
                    }
                }

                // one coloured pill per subject
                if (teacher.subjectNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        teacher.subjectNames.forEach { subject ->
                            val c = subjectColor(subject)
                            Box(
                                modifier = Modifier
                                    .background(c.copy(alpha = 0.13f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = c,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (materialCount == 1) "1 material" else "$materialCount materials",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // "view" pill with the app gradient
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(NenaPrimary, NenaSecondary)),
                                RoundedCornerShape(50)
                            )
                            .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}