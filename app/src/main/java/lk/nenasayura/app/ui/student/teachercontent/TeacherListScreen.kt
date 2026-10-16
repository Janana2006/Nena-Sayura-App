package lk.nenasayura.app.ui.student.teachercontent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

/**
 * Student screen "My teacher": every teacher with their name, stream and subjects.
 * Free teachers are listed first, paid teachers below them. Tapping a teacher opens that
 * teacher's materials.
 */
@Composable
fun TeacherListScreen(
    onBack: () -> Unit,
    onOpenTeacher: (String) -> Unit,
    viewModel: TeacherListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

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
                    text = "Pick a teacher and start learning",
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
                    message = "No teachers have joined yet.",
                    modifier = Modifier.weight(1f)
                )

                else -> {
                    val q = query.trim().lowercase()
                    fun matches(t: TeacherProfile): Boolean =
                        q.isEmpty() ||
                                t.name.lowercase().contains(q) ||
                                t.streamName.lowercase().contains(q) ||
                                t.subjectNames.any { it.lowercase().contains(q) }

                    val free = state.freeTeachers.filter { matches(it) }
                    val paid = state.paidTeachers.filter { matches(it) }

                    if (free.isEmpty() && paid.isEmpty()) {
                        EmptyView(
                            message = "No teachers match \"$query\".",
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

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Search teacher, stream or subject") },
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
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .padding(horizontal = 9.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TeacherCard(
    teacher: TeacherProfile,
    alreadyPaid: Boolean,
    onClick: () -> Unit
) {
    val isPaid = teacher.isPaidTeacher
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)) {
            // coloured edge: teal = free, gold = paid
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isPaid) NenaAccent else NenaSecondary)
            )
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeacherAvatar(name = teacher.name, size = 54)
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
                        Spacer(modifier = Modifier.height(3.dp))
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
                    if (teacher.subjectNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = teacher.subjectNames.joinToString("  \u2022  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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