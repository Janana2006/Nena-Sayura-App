package lk.nenasayura.app.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.viewmodel.SetupViewModel

@Composable
fun TeacherSetupScreen(
    modifier: Modifier,
    viewModel: SetupViewModel,
    onDone: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val blue = Color(0xFF0066FF)

    // Setup iwara karanakan back eken yanna denna epa
    BackHandler(enabled = true) { }

    LaunchedEffect(Unit) { viewModel.start() }

    LaunchedEffect(state.done) {
        if (state.done) {
            viewModel.consumeDone()
            onDone()
        }
    }

    val selectedStream = state.streams.find { it.id == state.selectedStreamId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("What do you teach?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = blue)
        Spacer(modifier = Modifier.height(6.dp))
        // CHANGED: "subjects" wenuwata "one subject"
        Text(
            "Select your stream, then choose the one subject you teach.",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))

        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            state.streams.isEmpty() -> {
                Text(state.error ?: "No streams found.", color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.loadStreams() }) { Text("Retry") }
            }

            else -> {
                // ---------- Streams ----------
                state.streams.forEach { stream ->
                    val selected = stream.id == state.selectedStreamId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) blue else Color.LightGray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectStream(stream.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.selectStream(stream.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stream.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // ---------- Selected stream eke subjects (ekak witharai select karanna puluwan) ----------
                if (selectedStream != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Select one subject in ${selectedStream.name}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    selectedStream.subjects.forEach { subject ->
                        val checked = subject.id in state.selectedSubjectIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectSubject(subject.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CHANGED: Checkbox wenuwata RadioButton
                            RadioButton(
                                selected = checked,
                                onClick = { viewModel.selectSubject(subject.id) }
                            )
                            Text(subject.name, fontSize = 16.sp)
                        }
                    }
                }

                state.error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color.Red, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Subject ekak select karanakan disabled
                Button(
                    onClick = { viewModel.confirm() },
                    enabled = state.selectedSubjectIds.isNotEmpty() && !state.saving,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = blue, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(if (state.saving) "Saving..." else "Confirm", fontSize = 17.sp)
                }
            }
        }

        TextButton(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Logout") }

        Spacer(modifier = Modifier.height(16.dp))
    }
}