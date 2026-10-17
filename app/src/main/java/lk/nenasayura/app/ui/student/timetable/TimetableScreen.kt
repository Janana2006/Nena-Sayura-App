package lk.nenasayura.app.ui.student.timetable

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.data.model.StudySession
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import lk.nenasayura.app.ui.theme.NenaPrimary

/**
 * The student's Timetable: the generate button, the planned sessions by day,
 * and the saved plan settings. (Day and Week views come in a later stage.)
 */
@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    onSetup: () -> Unit,
    onOpenQuiz: (quizId: String) -> Unit,
    viewModel: TimetableViewModel = viewModel()
)  {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = state.errorMessage
    val settings = state.settings

    // Loads when the screen opens, and again after coming back from the setup
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Scaffold(
        topBar = { NenaTopBar(title = "Timetable", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when {
            state.isLoading -> LoadingView(modifier)

            errorMessage != null && settings == null -> ErrorView(
                message = errorMessage,
                onRetry = viewModel::load,
                modifier = modifier
            )

            settings == null -> EmptyView(
                message = "You do not have a study plan yet. Answer a few questions and " +
                        "Nena Sayura will build a timetable around your subjects and free time.",
                modifier = modifier,
                actionLabel = "Create my plan",
                onAction = onSetup
            )

            else -> TimetableContent(
                state = state,
                settings = settings,
                onGenerate = viewModel::generate,
                onEdit = onSetup,
                onOpenQuiz = onOpenQuiz,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun TimetableContent(
    state: TimetableUiState,
    settings: StudyPlanSettings,
    onGenerate: () -> Unit,
    onEdit: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessionsByDay = state.sessions.groupBy { it.date }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "generate") {
            GenerateCard(
                settings = settings,
                isGenerating = state.isGenerating,
                error = state.generateError,
                onGenerate = onGenerate,
                onEdit = onEdit
            )
        }

        if (sessionsByDay.isNotEmpty()) {
            item(key = "heading") {
                Text(
                    text = "Your next weeks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            sessionsByDay.forEach { (date, daySessions) ->
                item(key = date) {
                    DayCard(
                        date = date,
                        sessions = daySessions,
                        onOpen = { session -> openSessionResource(context, session, onOpenQuiz) }
                    )
                }
            }
        } else if (settings.hasPlan) {
            item(key = "empty") {
                Text(
                    text = "No sessions are planned from today. Check your exam date, or regenerate your timetable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "settings") {
            SettingsCard(settings = settings, subjectNames = state.subjectNames)
        }
    }
}

@Composable
private fun GenerateCard(
    settings: StudyPlanSettings,
    isGenerating: Boolean,
    error: String?,
    onGenerate: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (settings.hasPlan) "Your study timetable" else "Ready to build your timetable",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (settings.hasPlan) {
                    "Planned until ${formatDay(settings.generatedUntil)}. Regenerating replaces the planned " +
                            "sessions from tomorrow; done and missed ones stay."
                } else {
                    "Nena Sayura will plan the next 4 weeks from your settings, starting tomorrow."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onGenerate,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (settings.hasPlan) "Regenerate timetable" else "Generate my timetable")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onEdit,
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Edit settings")
            }
        }
    }
}

@Composable
private fun DayCard(date: String, sessions: List<StudySession>, onOpen: (StudySession) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = formatDay(date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            sessions.forEach { session ->
                val hasResource = session.resourceId.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(session) },
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "${formatClock(session.startMinute)} - ${formatClock(session.startMinute + session.durationMinutes)}",
                        modifier = Modifier.width(132.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.subjectName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = session.kind.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (hasResource) session.resourceTitle else "No resource yet - self-study",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasResource) NenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (hasResource) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open resource",
                            modifier = Modifier.size(18.dp),
                            tint = NenaPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(settings: StudyPlanSettings, subjectNames: Map<String, String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Your plan settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            InfoRow(
                label = "Subjects",
                value = settings.confidence.entries.joinToString("\n") { (id, level) ->
                    "${subjectNames[id] ?: id}: ${Confidence.fromLevel(level).label}"
                }
            )
            InfoRow(
                label = "Study days",
                value = settings.availableDays.joinToString("\n") { day ->
                    "${fullDayName(day)}: ${formatDuration(settings.dailyMinutes[day] ?: 0)}"
                }
            )
            InfoRow(
                label = "Preferred time",
                value = "${settings.preferredWindow.label}, from ${formatClock(settings.preferredWindow.startMinute)}"
            )
            InfoRow(label = "Session length", value = "${settings.sessionMinutes} min")
            InfoRow(label = "Exam", value = examText(settings.examDateMillis))
            InfoRow(label = "Learning style", value = settings.learningStyle.label)
            InfoRow(label = "Goal", value = settings.goal.label)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** "2026-10-19" -> "Monday, 19 Oct" */
private fun formatDay(date: String): String =
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault()))

private fun examText(examDateMillis: Long): String {
    if (examDateMillis <= 0L) return "Not set"
    val days = daysUntilExam(examDateMillis)
    val left = when {
        days > 1 -> "$days days left"
        days == 1L -> "1 day left"
        days == 0L -> "today"
        else -> "date has passed"
    }
    return "${formatExamDate(examDateMillis)} ($left)"
}