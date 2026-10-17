package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer

/** What a student can do with one session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSheet(
    session: StudySession,
    onDismiss: () -> Unit,
    onOpenResource: () -> Unit,
    onSetStatus: (SessionStatus) -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = sessionTitle(session),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sessionSubtitle(session),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatDayLong(session.date)}  •  ${timeRange(session)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (session.wasMoved) {
                Text(
                    text = "Moved from ${formatDayLong(session.originalDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            if (hasResource(session)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = NenaPrimaryContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Study with",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF3D4660)
                        )
                        Text(
                            text = session.resourceTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B1B1F)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = onOpenResource) { Text("Open resource") }
                    }
                }
            } else if (session.kind != SessionKind.CUSTOM) {
                Text(
                    text = "No resource has been added for this subject yet. Use this time for self-study.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Status: ${session.status.label}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSetStatus(SessionStatus.DONE) },
                    enabled = session.status != SessionStatus.DONE,
                    modifier = Modifier.weight(1f)
                ) { Text("Done") }
                OutlinedButton(
                    onClick = { onSetStatus(SessionStatus.MISSED) },
                    enabled = session.status != SessionStatus.MISSED,
                    modifier = Modifier.weight(1f)
                ) { Text("Missed") }
            }
            if (session.status != SessionStatus.PLANNED) {
                TextButton(onClick = { onSetStatus(SessionStatus.PLANNED) }) { Text("Back to planned") }
            }
            TextButton(onClick = onMove) { Text("Move to another day") }
            if (session.kind == SessionKind.CUSTOM) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete this session") }
            }
        }
    }
}