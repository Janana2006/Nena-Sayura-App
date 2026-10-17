package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.SessionKind
import lk.nenasayura.app.data.model.SessionStatus
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.ui.theme.NenaPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- small helpers shared by the Day view, Week view and the session sheet ----------

internal val DoneGreen = Color(0xFF2E7D32)
internal val MissedRed = Color(0xFFC62828)

internal fun formatDayLong(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))

internal fun formatDayLong(date: String): String = formatDayLong(LocalDate.parse(date))

internal fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))

internal fun timeRange(session: StudySession): String =
    "${formatClock(session.startMinute)} - ${formatClock(session.startMinute + session.durationMinutes)}"

/** The student's own sessions show their title; generated ones show the subject. */
internal fun sessionTitle(session: StudySession): String =
    if (session.kind == SessionKind.CUSTOM) session.resourceTitle.ifBlank { "My session" } else session.subjectName

internal fun sessionSubtitle(session: StudySession): String =
    if (session.kind == SessionKind.CUSTOM) {
        listOfNotNull(session.subjectName.ifBlank { null }, "My session").joinToString(" • ")
    } else session.kind.label

internal fun hasResource(session: StudySession): Boolean =
    session.kind != SessionKind.CUSTOM && session.resourceId.isNotBlank()

// ---------- the Day view ----------

@Composable
fun DayView(
    state: TimetableUiState,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSession: (StudySession) -> Unit,
    onToggleDone: (StudySession) -> Unit,
    onOpenResource: (StudySession) -> Unit,
    onAddSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions = state.daySessions
    val datesWithSessions = state.weekSessions.map { it.date }.toSet()

    Column(modifier = modifier) {
        DayStrip(
            days = state.weekDays,
            selected = state.selectedDate,
            datesWithSessions = datesWithSessions,
            onSelect = onSelectDate
        )

        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nothing is planned for ${formatDayLong(state.selectedDate)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A rest day, or add something of your own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onAddSession) { Text("Add my own session") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sessions, key = { _, session -> session.id }) { index, session ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (index > 0) {
                            val previous = sessions[index - 1]
                            BreakLine(minutes = session.startMinute - (previous.startMinute + previous.durationMinutes))
                        }
                        SessionCard(
                            session = session,
                            onClick = { onOpenSession(session) },
                            onToggleDone = { onToggleDone(session) },
                            onOpenResource = { onOpenResource(session) }
                        )
                    }
                }
                item(key = "flex") {
                    Text(
                        text = "Flex time: finish anything that is still unfinished today.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakLine(minutes: Int) {
    if (minutes <= 0) return
    Text(
        text = "Break  •  $minutes min",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DayStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    datesWithSessions: Set<String>,
    onSelect: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            Surface(
                onClick = { onSelect(day) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) NenaPrimary else MaterialTheme.colorScheme.surface,
                border = if (day == today && !isSelected) BorderStroke(1.5.dp, NenaPrimary) else null,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = shortDayName(day.dayOfWeek.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    // A dot marks the days that have sessions
                    val dotColor = when {
                        day.toString() !in datesWithSessions -> Color.Transparent
                        isSelected -> Color.White
                        else -> NenaPrimary
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(dotColor, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: StudySession,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onOpenResource: () -> Unit
) {
    val done = session.status == SessionStatus.DONE

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleDone) {
                Icon(
                    imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (done) "Mark as not done" else "Mark as done",
                    tint = if (done) DoneGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeRange(session),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sessionTitle(session),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = sessionSubtitle(session),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.kind != SessionKind.CUSTOM) {
                    Text(
                        text = if (hasResource(session)) session.resourceTitle else "No resource yet - self-study",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasResource(session)) NenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.status == SessionStatus.MISSED) {
                    Text(
                        text = "Missed",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MissedRed
                    )
                }
                if (session.wasMoved) {
                    Text(
                        text = "Moved from ${formatDayLong(session.originalDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hasResource(session)) {
                IconButton(onClick = onOpenResource) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open resource",
                        tint = NenaPrimary
                    )
                }
            }
        }
    }
}