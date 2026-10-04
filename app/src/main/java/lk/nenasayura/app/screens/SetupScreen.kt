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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.viewmodel.SetupViewModel

private val InkText = Color(0xFF1B1B1F)
private val GreyText = Color(0xFF6B7280)

@Composable
fun SetupScreen(
    modifier: Modifier,
    viewModel: SetupViewModel,
    role: String,
    onDone: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isTeacher = role == "teacher"

    // Setup iwara karanakan back eken yanna denna epa
    BackHandler(enabled = true) { }

    LaunchedEffect(role) { viewModel.start(role) }

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

    Box(modifier = Modifier.fillMaxSize().background(NenaBackground)) {

        // Guest home eke wage gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(
                    brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header content
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

            // Progress pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (selectedStream == null) "Step 1 · Choose a stream"
                        else "Step 2 · Choose subjects",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (selectedStream != null) {
                        Text(
                            text = "$doneCount / $totalNeeded",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
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
                    Text("Your stream", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkText)
                    Spacer(modifier = Modifier.height(10.dp))

                    state.streams.forEach { stream ->
                        val selected = stream.id == state.selectedStreamId
                        StreamCard(
                            name = stream.name,
                            selected = selected,
                            onClick = { viewModel.selectStream(stream.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // ---------- Subjects ----------
                    if (selectedStream != null) {
                        Spacer(modifier = Modifier.height(14.dp))

                        if (state.hasGroups) {
                            // ===== Slots (Subject 1, 2, 3) =====
                            Text(
                                text = "Your ${selectedStream.groups.size} subjects in ${selectedStream.name}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkText
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            selectedStream.groups.forEachIndexed { index, group ->
                                val compulsory = group.size == 1

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(28.dp).background(NenaPrimary, CircleShape),
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
                                        Spacer(modifier = Modifier.height(10.dp))

                                        group.forEach { subject ->
                                            val checked = subject.id in state.selectedSubjectIds
                                            SubjectRow(
                                                name = subject.name,
                                                selected = if (compulsory) true else checked,
                                                enabled = !compulsory,
                                                onClick = { viewModel.toggleSubject(subject.id) }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            // ===== Slots nathi streams =====
                            Text(
                                text = if (isTeacher) "Select one subject in ${selectedStream.name}"
                                else "Select $need subjects in ${selectedStream.name}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkText
                            )
                            if (limitReached) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "You can select only $need subjects. Untick one to change.",
                                    fontSize = 12.sp,
                                    color = GreyText
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
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
                                            onClick = { viewModel.toggleSubject(subject.id) }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    state.error?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = Color(0xFFC62828), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.confirm() },
                        enabled = state.canConfirm && !state.saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NenaPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFD5D9E6),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Logout",
                    color = GreyText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ---------- Stream card ----------
@Composable
private fun StreamCard(name: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (selected) NenaPrimary else Color(0xFFE3E6EF), label = "streamBorder")
    val bg by animateColorAsState(if (selected) NenaPrimaryContainer else Color.White, label = "streamBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (selected) Brush.linearGradient(listOf(NenaPrimary, NenaSecondary))
                    else Brush.linearGradient(listOf(NenaPrimaryContainer, NenaPrimaryContainer)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = if (selected) Color.White else NenaPrimary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) NenaPrimary else InkText,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NenaSecondary)
        }
    }
}

// ---------- Subject row (check circle ekka) ----------
@Composable
private fun SubjectRow(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) NenaPrimaryContainer else NenaBackground, label = "subjectBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled || selected) InkText else Color(0xFF9AA0AE),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (selected) Modifier.background(NenaSecondary, CircleShape)
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