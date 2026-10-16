package lk.nenasayura.app.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.LoginMethod
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary

// ---------------------------------------------------------------------------
// Filters
// ---------------------------------------------------------------------------

private enum class UserGroup(val label: String, val icon: ImageVector) {
    Students("Students", Icons.Filled.School),
    Teachers("Teachers", Icons.Filled.Person)
}

private enum class TeacherType(val label: String) {
    All("All teachers"),
    Free("Free"),
    Paid("Paid")
}

private enum class LoginFilter(val label: String, val method: LoginMethod?) {
    All("All sign-ins", null),
    Email("Email", LoginMethod.Email),
    Google("Google", LoginMethod.Google)
}

// ---------------------------------------------------------------------------
// Theme aware tones (readable in BOTH light and dark mode)
// ---------------------------------------------------------------------------

private data class Tone(val container: Color, val content: Color)

@Composable
private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
private fun blueTone(): Tone =
    if (isDarkTheme()) Tone(Color(0xFF28408F), Color(0xFFDCE4FF)) else Tone(Color(0xFFDCE4FF), Color(0xFF2F4EA8))

@Composable
private fun tealTone(): Tone =
    if (isDarkTheme()) Tone(Color(0xFF0F4F47), Color(0xFFCDEDE7)) else Tone(Color(0xFFCDEDE7), Color(0xFF00695C))

@Composable
private fun amberTone(): Tone =
    if (isDarkTheme()) Tone(Color(0xFF5A4300), Color(0xFFFFE9B8)) else Tone(Color(0xFFFFE9B8), Color(0xFF6B4A00))

@Composable
private fun googleTone(): Tone =
    if (isDarkTheme()) Tone(Color(0xFF4A2A28), Color(0xFFFFDAD6)) else Tone(Color(0xFFFDE8E6), Color(0xFFB3261E))

@Composable
private fun greyTone(): Tone =
    if (isDarkTheme()) Tone(Color(0xFF2A2F42), Color(0xFFB0B6C8)) else Tone(Color(0xFFEEF0F6), Color(0xFF5B6478))

private val GoogleRed = Color(0xFFDB4437)
private val UnknownGrey = Color(0xFF8A93A8)

private fun LoginMethod.badgeColor(): Color = when (this) {
    LoginMethod.Google -> GoogleRed
    LoginMethod.Email -> NenaPrimary
    LoginMethod.Unknown -> UnknownGrey
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/** Users tab: every student and teacher, with sign-in method (Email / Google) and Free / Paid teachers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdminUsersTab(
    users: List<AppUser>,
    payments: List<Payment>,
    modifier: Modifier = Modifier
) {
    var group by remember { mutableStateOf(UserGroup.Students) }
    var teacherType by remember { mutableStateOf(TeacherType.All) }
    var loginFilter by remember { mutableStateOf(LoginFilter.All) }
    var query by remember { mutableStateOf("") }

    val students = remember(users) { users.filter { it.isStudent } }
    val teachers = remember(users) { users.filter { it.isTeacher } }

    val base = if (group == UserGroup.Students) students else teachers.filter {
        when (teacherType) {
            TeacherType.All -> true
            TeacherType.Free -> it.isFree
            TeacherType.Paid -> !it.isFree
        }
    }

    val shown = base.filter { u ->
        (loginFilter.method == null || u.login == loginFilter.method) &&
                (query.isBlank() ||
                        u.name.contains(query, ignoreCase = true) ||
                        u.email.contains(query, ignoreCase = true))
    }

    val everyone = students + teachers

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OverviewCard(
                total = everyone.size,
                students = students.size,
                teachers = teachers.size,
                google = everyone.count { it.login == LoginMethod.Google },
                email = everyone.count { it.login == LoginMethod.Email },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            )
        }

        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GroupSwitch(
                    selected = group,
                    counts = mapOf(
                        UserGroup.Students to students.size,
                        UserGroup.Teachers to teachers.size
                    ),
                    onSelect = { group = it }
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search by name or email") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (group == UserGroup.Teachers) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TeacherType.entries.forEach { t ->
                            val n = when (t) {
                                TeacherType.All -> teachers.size
                                TeacherType.Free -> teachers.count { it.isFree }
                                TeacherType.Paid -> teachers.count { !it.isFree }
                            }
                            FilterPill(
                                label = "${t.label} · $n",
                                selected = teacherType == t,
                                modifier = Modifier.weight(1f),
                                onClick = { teacherType = t }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LoginFilter.entries.forEach { f ->
                        FilterPill(
                            label = f.label,
                            selected = loginFilter == f,
                            modifier = Modifier.weight(1f),
                            leading = f.method?.let { m ->
                                { MethodGlyph(m, size = 14.dp, tint = m.badgeColor()) }
                            },
                            onClick = { loginFilter = f }
                        )
                    }
                }

                Text(
                    text = "Showing ${shown.size} of ${base.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (shown.isEmpty()) {
            item {
                EmptyView(
                    message = "No users found.",
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        } else {
            items(shown, key = { it.uid }) { user ->
                UserCard(
                    user = user,
                    payments = payments,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Overview header
// ---------------------------------------------------------------------------

@Composable
private fun OverviewCard(
    total: Int,
    students: Int,
    teachers: Int,
    google: Int,
    email: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2F4EA8), Color(0xFF00796B))))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "COMMUNITY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "registered users",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewStat("Students", students)
                OverviewStat("Teachers", teachers)
                OverviewStat("Google", google)
                OverviewStat("Email", email)
            }
        }
    }
}

@Composable
private fun OverviewStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

@Composable
private fun GroupSwitch(
    selected: UserGroup,
    counts: Map<UserGroup, Int>,
    onSelect: (UserGroup) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surfaceVariant)
            .padding(4.dp)
    ) {
        UserGroup.entries.forEach { g ->
            val sel = g == selected
            val bg by animateColorAsState(if (sel) cs.surface else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (sel) cs.primary else cs.onSurfaceVariant, label = "segFg")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelect(g) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(g.icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = g.label,
                    color = fg,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(fg.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = (counts[g] ?: 0).toString(),
                        color = fg,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else cs.surface, label = "pillBg")
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(
            1.dp,
            if (selected) cs.primary.copy(alpha = 0.55f) else cs.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// User card
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCard(user: AppUser, payments: List<Payment>, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(user)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name.ifBlank { "No name" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign-in method + teacher type
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoginChip(user.login)
                if (user.isTeacher) {
                    if (user.isFree) {
                        Tag("Free teacher", tealTone())
                    } else {
                        Tag("Paid teacher · ${money(user.fee.toDouble())}", amberTone())
                    }
                }
            }

            // Stream + subjects
            if (user.streamName.isNotBlank() || user.subjectNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (user.streamName.isNotBlank()) {
                        Tag("${user.streamName} Stream", blueTone())
                    }
                    user.subjectNames.filter { it.isNotBlank() }.forEach { subject ->
                        SubjectChip(subject)
                    }
                }
            }

            // Paid teachers: how many students paid them and how much
            if (user.isTeacher && !user.isFree) {
                val paid = remember(payments, user.uid) {
                    payments.filter { it.teacherId == user.uid && it.isPaid }
                }
                val tone = amberTone()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tone.container.copy(alpha = if (isDarkTheme()) 0.45f else 0.6f))
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniStat(
                        label = "Paid students",
                        value = paid.map { it.studentId }.distinct().size.toString(),
                        color = tone.content,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(tone.content.copy(alpha = 0.25f))
                    )
                    MiniStat(
                        label = "Total received",
                        value = money(paid.sumOf { it.amount }),
                        color = tone.content,
                        modifier = Modifier
                            .weight(1.4f)
                            .padding(start = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

/** Round avatar, coloured by role, with a small badge showing how the user signs in. */
@Composable
private fun Avatar(user: AppUser) {
    val colors = when {
        user.isTeacher && !user.isFree -> listOf(Color(0xFFF2A100), Color(0xFFE65100))
        user.isTeacher -> listOf(NenaSecondary, Color(0xFF26A69A))
        else -> listOf(NenaPrimary, Color(0xFF5C7CFA))
    }
    Box(modifier = Modifier.size(54.dp)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopStart)
                .background(Brush.linearGradient(colors), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(2.dp)
                .background(user.login.badgeColor(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MethodGlyph(user.login, size = 12.dp, tint = Color.White)
        }
    }
}

/** "G" for Google, mail icon for Email, info icon when it is not recorded yet. */
@Composable
private fun MethodGlyph(method: LoginMethod, size: androidx.compose.ui.unit.Dp, tint: Color) {
    when (method) {
        LoginMethod.Google -> Text(
            text = "G",
            color = tint,
            fontWeight = FontWeight.Black,
            fontSize = (size.value - 1).sp,
            lineHeight = size.value.sp
        )

        LoginMethod.Email -> Icon(
            imageVector = Icons.Filled.Email,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size)
        )

        LoginMethod.Unknown -> Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun LoginChip(method: LoginMethod) {
    val tone = when (method) {
        LoginMethod.Google -> googleTone()
        LoginMethod.Email -> blueTone()
        LoginMethod.Unknown -> greyTone()
    }
    val text = when (method) {
        LoginMethod.Google -> "Google login"
        LoginMethod.Email -> "Email login"
        LoginMethod.Unknown -> "Login not recorded"
    }
    Tag(
        text = text,
        tone = tone,
        leading = { MethodGlyph(method, size = 14.dp, tint = tone.content) }
    )
}

@Composable
private fun Tag(text: String, tone: Tone, leading: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tone.container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tone.content
        )
    }
}

@Composable
private fun SubjectChip(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = cs.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(cs.surfaceVariant)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    )
}