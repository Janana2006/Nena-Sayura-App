package lk.nenasayura.app.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.viewmodel.SetupViewModel

@Composable
fun SetupScreen(
    modifier: Modifier,
    viewModel: SetupViewModel,
    role: String,
    onDone: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val primaryBlue = Color(0xFF2563EB)
    val lightBlueBg = Color(0xFFF0F5FF)
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF), Color(0xFFFFFFFF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isTeacher) "What do you teach?" else "What are you studying?",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E3A8A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isTeacher) "Select your stream, then choose the one subject you teach."
                else "Select your A/L stream, then choose your 3 subjects.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = primaryBlue) }
                }

                state.streams.isEmpty() -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error ?: "No streams found.", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadStreams() },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Retry") }
                        }
                    }
                }

                else -> {
                    // ---------- Streams ----------
                    state.streams.forEach { stream ->
                        val selected = stream.id == state.selectedStreamId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) lightBlueBg else Color.White,
                            shadowElevation = if (selected) 4.dp else 2.dp,
                            tonalElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) primaryBlue else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.selectStream(stream.id) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { viewModel.selectStream(stream.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryBlue)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stream.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) primaryBlue else Color(0xFF1E293B)
                                )
                            }
                        }
                    }

                    // ---------- Subjects ----------
                    if (selectedStream != null) {
                        Spacer(modifier = Modifier.height(20.dp))

                        if (state.hasGroups) {
                            // ===== Slots (Subject 1, 2, 3) =====
                            val done = selectedStream.groups.count { group ->
                                group.any { it.id in state.selectedSubjectIds }
                            }
                            Text(
                                text = "Your ${selectedStream.groups.size} subjects in ${selectedStream.name} ($done/${selectedStream.groups.size})",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            selectedStream.groups.forEachIndexed { index, group ->
                                val compulsory = group.size == 1

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Subject ${index + 1} · " + if (compulsory) "Compulsory" else "Choose one",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryBlue
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        group.forEach { subject ->
                                            val checked = subject.id in state.selectedSubjectIds

                                            if (compulsory) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(checked = true, onCheckedChange = null, enabled = false)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(subject.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { viewModel.toggleSubject(subject.id) }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = checked,
                                                        onClick = { viewModel.toggleSubject(subject.id) },
                                                        colors = RadioButtonDefaults.colors(selectedColor = primaryBlue)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(subject.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ===== Slots nathi streams =====
                            Text(
                                text = if (isTeacher) "Select one subject in ${selectedStream.name}"
                                else "Select $need subjects in ${selectedStream.name} ($picked/$need)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                            if (limitReached) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "You can select only $need subjects. Untick one to change.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    selectedStream.subjects.forEach { subject ->
                                        val checked = subject.id in state.selectedSubjectIds
                                        val enabled = isTeacher || checked || !limitReached

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable(enabled = enabled) { viewModel.toggleSubject(subject.id) }
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isTeacher) {
                                                RadioButton(
                                                    selected = checked,
                                                    onClick = { viewModel.toggleSubject(subject.id) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = primaryBlue)
                                                )
                                            } else {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { viewModel.toggleSubject(subject.id) },
                                                    enabled = enabled
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = subject.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (enabled) Color(0xFF1E293B) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    state.error?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { viewModel.confirm() },
                        enabled = state.canConfirm && !state.saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(
                            text = if (state.saving) "Saving..." else "Confirm & Continue",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onLogout,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Logout",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
