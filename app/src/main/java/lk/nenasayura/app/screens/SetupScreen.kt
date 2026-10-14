package lk.nenasayura.app.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.viewmodel.SetupViewModel

private val InkText = Color(0xFF1B1B1F)
private val GreyText = Color(0xFF6B7280)

// Subject 1, 2, 3 walata wen wen colours
private val SlotColors = listOf(NenaPrimary, NenaSecondary, NenaAccent)

@Composable
fun SetupScreen(
    modifier: Modifier,
    viewModel: SetupViewModel,
    role: String,
    onDone: () -> Unit,
    onLogout: () -> Unit,
    // --- "Become a teacher / student" mode (account already exists with the other role) ---
    addRole: Boolean = false,
    isFree: Boolean = true,
    fee: Int = 0,
    bottomButtonLabel: String = "Logout",
    blockBack: Boolean = true
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTeacher = role == "teacher"

    // Setup iwara karanakan back eken yanna denna epa
    BackHandler(enabled = blockBack) { }

    LaunchedEffect(role) { viewModel.start(role, addRole, isFree, fee) }

    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.consumeDone()
            onDone()
        }
    }

    val selectedStream = state.selectedStream
    val need = state.requiredCount
    val picked = state.selectedSubjectIds.size
    val limitReached = !isTeacher && !state.hasGroups && picked >= need

    // Progress (header eke penna)
    val totalNeeded = when {
        selectedStream == null -> 0
        state.hasGroups -> selectedStream.groups.size
        isTeacher -> 1
        else -> need
    }
    val doneCount = when {
        selectedStream == null -> 0
        state.hasGroups -> selectedStream.groups.count { g -> g.any { it.id in state.selectedSubjectIds } }
        else -> picked
    }

    // Root: header + content scroll wenawa, Confirm bar eka yatin sticky
    Column(modifier = Modifier.fillMaxSize().background(NenaBackground)) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {

            // ================= Header (content ekata anuwa height wenas wenawa) =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)))
            ) {
                // Alankara walalu
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 60.dp, y = (-50).dp)
                        .size(180.dp)
                        .background(Color.White.copy(alpha = 0.07f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-50).dp, y = 40.dp)
                        .size(140.dp)
                        .background(Color.White.copy(alpha = 0.06f), CircleShape)
                )

                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp).background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTeacher) Icons.Default.Person else Icons.Default.School,
                                    contentDescription = null,
                                    tint = NenaPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (isTeacher) "What do you teach?" else "What are you studying?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isTeacher) "Pick your stream and your subject"
                                else "Pick your A/L stream and 3 subjects",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stepper + progress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StepItem(
                                number = 1,
                                label = "Stream",
                                active = selectedStream == null,
                                done = selectedStream != null
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                            )
                            StepItem(
                                number = 2,
                                label = "Subjects",
                                active = selectedStream != null && !state.canConfirm,
                                done = state.canConfirm
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedStream == null) "Choose a stream to start"
                                else "Choose your subjects",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            if (selectedStream != null) {
                                Text(
                                    text = "$doneCount / $totalNeeded",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                when {
                                    selectedStream == null -> 0.05f
                                    totalNeeded == 0 -> 0f
                                    else -> (doneCount.toFloat() / totalNeeded).coerceIn(0f, 1f)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            // ================= Content =================
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(22.dp))

                when {
                    state.loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = NenaPrimary) }
                    }

                    state.streams.isEmpty() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE0DD))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    state.error ?: "No streams found.",
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.loadStreams() },
                                    colors = ButtonDefaults.buttonColors(containerColor = NenaPrimary),
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text("Retry") }
                            }
                        }
                    }

                    else -> {
                        // ---------- Streams ----------
                        SectionTitle(title = "Your stream", subtitle = "Tap the stream you are studying")
                        Spacer(modifier = Modifier.height(12.dp))

                        state.streams.forEach { stream ->
                            StreamCard(
                                name = stream.name,
                                subjectCount = stream.subjects.size,
                                selected = stream.id == state.selectedStreamId,
                                onClick = { viewModel.selectStream(stream.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // ---------- Subjects ----------
                        if (selectedStream != null) {
                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.hasGroups) {
                                // ===== Slots (Subject 1, 2, 3) =====
                                SectionTitle(
                                    title = "Your ${selectedStream.groups.size} subjects",
                                    subtitle = "in ${selectedStream.name}"
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                selectedStream.groups.forEachIndexed { index, group ->
                                    val compulsory = group.size == 1
                                    val accent = SlotColors[index % SlotColors.size]

                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier.size(30.dp).background(accent, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    "Subject ${index + 1}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = InkText,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (compulsory) NenaSecondaryContainer else NenaAccentContainer,
                                                            RoundedCornerShape(50)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (compulsory) "Compulsory" else "Choose one",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (compulsory) NenaSecondary else Color(0xFF8A5A00)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))

                                            group.forEach { subject ->
                                                val checked = subject.id in state.selectedSubjectIds
                                                SubjectRow(
                                                    name = subject.name,
                                                    selected = if (compulsory) true else checked,
                                                    enabled = !compulsory,
                                                    accent = accent,
                                                    onClick = { viewModel.toggleSubject(subject.id) }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ===== Slots nathi streams =====
                                SectionTitle(
                                    title = if (isTeacher) "Select one subject" else "Select $need subjects",
                                    subtitle = "in ${selectedStream.name}"
                                )
                                if (limitReached) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You can select only $need subjects. Untick one to change.",
                                        fontSize = 12.sp,
                                        color = GreyText
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        selectedStream.subjects.forEach { subject ->
                                            val checked = subject.id in state.selectedSubjectIds
                                            val enabled = isTeacher || checked || !limitReached

                                            SubjectRow(
                                                name = subject.name,
                                                selected = checked,
                                                enabled = enabled,
                                                accent = NenaPrimary,
                                                onClick = { viewModel.toggleSubject(subject.id) }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ================= Bottom bar (hama welawema penawa) =================
        Surface(color = Color.White, shadowElevation = 16.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                state.error?.let {
                    if (state.streams.isNotEmpty()) {
                        Text(
                            it,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Button(
                    onClick = { viewModel.confirm() },
                    enabled = state.canConfirm && !state.saving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NenaPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFD5D9E6),
                        disabledContentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("Saving...", fontSize = 16.sp)
                    } else {
                        Text("Confirm & Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                TextButton(onClick = onLogout) {
                    Text(
                        text = bottomButtonLabel,
                        color = GreyText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ---------- Section title ----------
@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkText)
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, color = GreyText)
        }
    }
}

// ---------- Header stepper item ----------
@Composable
private fun StepItem(number: Int, label: String, active: Boolean, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (active || done) Color.White else Color.White.copy(alpha = 0.25f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = NenaPrimary,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    "$number",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) NenaPrimary else Color.White
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (active || done) FontWeight.SemiBold else FontWeight.Medium,
            color = Color.White.copy(alpha = if (active || done) 1f else 0.7f)
        )
    }
}

// ---------- Icons (nama balala icon eka thorana) ----------
private fun streamIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        "math" in n -> Icons.Default.Calculate
        "bio" in n || "science" in n -> Icons.Default.Science
        "commerce" in n || "business" in n -> Icons.Default.Storefront
        "tech" in n -> Icons.Default.Build
        "art" in n -> Icons.Default.Palette
        else -> Icons.Default.School
    }
}

private fun subjectIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        "math" in n -> Icons.Default.Calculate
        "physic" in n -> Icons.Default.Bolt
        "chem" in n || "bio" in n || "science" in n -> Icons.Default.Science
        "information" in n || "ict" in n || "computer" in n -> Icons.Default.Computer
        "econom" in n || "business" in n || "account" in n -> Icons.Default.AccountBalance
        "art" in n || "drama" in n || "music" in n -> Icons.Default.Palette
        else -> Icons.Default.Book
    }
}

// ---------- Stream card ----------
@Composable
private fun StreamCard(name: String, subjectCount: Int, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (selected) NenaPrimary else Color(0xFFE3E6EF), label = "streamBorder")
    val bg by animateColorAsState(if (selected) NenaPrimaryContainer else Color.White, label = "streamBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (selected) Brush.linearGradient(listOf(NenaPrimary, NenaSecondary))
                    else Brush.linearGradient(listOf(NenaPrimaryContainer, NenaPrimaryContainer)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                streamIcon(name),
                contentDescription = null,
                tint = if (selected) Color.White else NenaPrimary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) NenaPrimary else InkText
            )
            if (subjectCount > 0) {
                Text(
                    text = "$subjectCount subjects",
                    fontSize = 12.sp,
                    color = GreyText
                )
            }
        }
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NenaSecondary)
        }
    }
}

// ---------- Subject row ----------
@Composable
private fun SubjectRow(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) accent.copy(alpha = 0.10f) else NenaBackground,
        label = "subjectBg"
    )
    val borderColor by animateColorAsState(
        if (selected) accent else Color(0xFFE6E9F2),
        label = "subjectBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled || selected) 1f else 0.5f)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                subjectIcon(name),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = InkText,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (selected) Modifier.background(accent, CircleShape)
                    else Modifier.border(2.dp, Color(0xFFC5CAD8), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}