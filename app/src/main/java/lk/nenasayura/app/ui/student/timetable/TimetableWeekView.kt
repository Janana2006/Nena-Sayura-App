package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.ui.theme.NenaPrimary
import java.time.LocalDate

/** The whole week at a glance. Tapping a day opens it in the Day view. */
@Composable
fun WeekView(
    state: TimetableUiState,
    onOpenDay: (LocalDate) -> Unit,
    onOpenSession: (StudySession) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(state.weekDays, key = { it.toString() }) { day ->
            val sessions = state.weekSessions
                .filter { it.date == day.toString() }
                .sortedBy { it.startMinute }
            val done = sessions.count { it.status == SessionStatus.DONE }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDay(day) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDayLong(day) + if (day == today) "  (Today)" else "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (day == today) NenaPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sessions.isEmpty()) "Rest day" else "$done of ${sessions.size} done",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (sessions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        sessions.forEach { session ->
                            WeekSessionRow(session = session, onClick = { onOpenSession(session) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSessionRow(session: StudySession, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (session.status) {
            SessionStatus.DONE -> Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = DoneGreen)
            SessionStatus.MISSED -> Icon(Icons.Filled.Cancel, null, Modifier.size(18.dp), tint = MissedRed)
            SessionStatus.PLANNED -> Icon(
                Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = formatClock(session.startMinute),
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sessionTitle(session),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sessionSubtitle(session),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}