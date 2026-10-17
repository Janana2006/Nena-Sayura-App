package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.StudyPlanSettings
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaAccentContainer

/**
 * The student's Timetable. For now it shows the saved plan settings;
 * the weekly timetable itself is added in the next stages.
 */
@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    onSetup: () -> Unit,
    viewModel: TimetableViewModel = viewModel()
) {
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

            else -> PlanSummary(
                settings = settings,
                subjectNames = state.subjectNames,
                onEdit = onSetup,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PlanSummary(
    settings: StudyPlanSettings,
    subjectNames: Map<String, String>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Edit settings")
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = NenaAccentContainer
        ) {
            Text(
                text = "Your weekly timetable will appear here once it is generated. That comes in the next step.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF3E2A00)
            )
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