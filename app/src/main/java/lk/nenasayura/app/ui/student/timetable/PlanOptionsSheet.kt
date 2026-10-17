package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.StudyPlanSettings

/** The plan options in a bottom sheet (opened from the settings button). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanOptionsSheet(
    settings: StudyPlanSettings,
    subjectNames: Map<String, String>,
    isGenerating: Boolean,
    error: String?,
    onGenerate: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        PlanOptionsContent(
            settings = settings,
            subjectNames = subjectNames,
            isGenerating = isGenerating,
            error = error,
            onGenerate = onGenerate,
            onEdit = onEdit,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        )
    }
}

/** The generate card and the saved settings. Also the whole screen before the first timetable exists. */
@Composable
fun PlanOptionsContent(
    settings: StudyPlanSettings,
    subjectNames: Map<String, String>,
    isGenerating: Boolean,
    error: String?,
    onGenerate: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GenerateCard(
            settings = settings,
            isGenerating = isGenerating,
            error = error,
            onGenerate = onGenerate,
            onEdit = onEdit
        )
        SettingsCard(settings = settings, subjectNames = subjectNames)
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
                    "Planned until ${formatDayLong(settings.generatedUntil)}. Regenerating replaces the " +
                            "planned sessions from tomorrow; done and missed ones stay."
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