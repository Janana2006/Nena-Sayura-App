package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.Confidence
import lk.nenasayura.app.data.model.LearningStyle
import lk.nenasayura.app.data.model.PreferredWindow
import lk.nenasayura.app.data.model.StudyGoal
import lk.nenasayura.app.data.model.Subject
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import java.time.LocalDate
import java.time.ZoneOffset

// ---------- Step 1: subjects ----------

@Composable
fun SubjectsStep(
    subjects: List<Subject>,
    confidence: Map<String, Int>,
    onChange: (subjectId: String, level: Int) -> Unit
) {
    HelpText("How do you feel about each subject? Subjects you struggle with get more study time.")
    Spacer(modifier = Modifier.height(12.dp))
    subjects.forEach { subject ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Confidence.entries.forEach { level ->
                        FilterChip(
                            selected = confidence[subject.id] == level.level,
                            onClick = { onChange(subject.id, level.level) },
                            label = { Text(level.label) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

// ---------- Step 2: days and hours ----------

@Composable
fun DaysStep(
    days: Set<Int>,
    minutes: Map<Int, Int>,
    onToggleDay: (Int) -> Unit,
    onChangeMinutes: (day: Int, delta: Int) -> Unit
) {
    HelpText("Which days can you study, and for how long?")
    Spacer(modifier = Modifier.height(12.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (1..7).forEach { day ->
            DayToggle(
                label = shortDayName(day),
                selected = day in days,
                onClick = { onToggleDay(day) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    (1..7).filter { it in days }.forEach { day ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fullDayName(day),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = { onChangeMinutes(day, -30) }) {
                Icon(Icons.Filled.Remove, contentDescription = "30 minutes less on ${fullDayName(day)}")
            }
            Text(
                text = formatDuration(minutes[day] ?: 0),
                modifier = Modifier.width(76.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onChangeMinutes(day, 30) }) {
                Icon(Icons.Filled.Add, contentDescription = "30 minutes more on ${fullDayName(day)}")
            }
        }
    }

    if (days.size == 7) {
        Spacer(modifier = Modifier.height(8.dp))
        HelpText("Tip: a rest day helps you remember more of what you study.")
    }
}

@Composable
private fun DayToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = if (selected) NenaPrimary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Step 3: when you study ----------

@Composable
fun TimeStep(
    window: PreferredWindow,
    sessionMinutes: Int,
    onWindowChange: (PreferredWindow) -> Unit,
    onSessionChange: (Int) -> Unit
) {
    HelpText("When do you like to study?")
    Spacer(modifier = Modifier.height(8.dp))
    PreferredWindow.entries.forEach { option ->
        OptionCard(
            title = option.label,
            subtitle = "Sessions start around ${formatClock(option.startMinute)}",
            selected = option == window,
            onClick = { onWindowChange(option) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Spacer(modifier = Modifier.height(12.dp))
    HelpText("How long should one study session be? Short sessions with breaks work better than one long block.")
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30, 45, 60).forEach { minutes ->
            FilterChip(
                selected = minutes == sessionMinutes,
                onClick = { onSessionChange(minutes) },
                label = { Text("$minutes min") }
            )
        }
    }
}

// ---------- Step 4: exam, style, goal ----------

@Composable
fun GoalStep(
    examDateMillis: Long,
    profileExamYear: Int,
    learningStyle: LearningStyle,
    goal: StudyGoal,
    onExamDateChange: (Long) -> Unit,
    onStyleChange: (LearningStyle) -> Unit,
    onGoalChange: (StudyGoal) -> Unit
) {
    HelpText("When is your exam?")
    Spacer(modifier = Modifier.height(8.dp))
    ExamDateField(
        examDateMillis = examDateMillis,
        profileExamYear = profileExamYear,
        onDateSelected = onExamDateChange
    )
    if (profileExamYear > 0) {
        Spacer(modifier = Modifier.height(4.dp))
        HelpText("Your profile says you sit the exam in $profileExamYear.")
    }

    Spacer(modifier = Modifier.height(20.dp))
    HelpText("How do you like to learn?")
    Spacer(modifier = Modifier.height(8.dp))
    LearningStyle.entries.forEach { style ->
        OptionCard(
            title = style.label,
            subtitle = when (style) {
                LearningStyle.VIDEO -> "More video lessons from teachers"
                LearningStyle.READING -> "More short notes and books"
                LearningStyle.PRACTICE -> "More quizzes and papers"
                LearningStyle.BALANCED -> "A mix of everything"
            },
            selected = style == learningStyle,
            onClick = { onStyleChange(style) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Spacer(modifier = Modifier.height(12.dp))
    HelpText("What is your main goal?")
    Spacer(modifier = Modifier.height(8.dp))
    StudyGoal.entries.forEach { option ->
        OptionCard(
            title = option.label,
            subtitle = when (option) {
                StudyGoal.UNDERSTAND -> "More learning time, less paper practice"
                StudyGoal.MARKS -> "A balance of learning and practice"
                StudyGoal.EXAM_READY -> "More papers and revision"
            },
            selected = option == goal,
            onClick = { onGoalChange(option) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDateField(
    examDateMillis: Long,
    profileExamYear: Int,
    onDateSelected: (Long) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(if (examDateMillis > 0L) formatExamDate(examDateMillis) else "Choose your exam date")
    }

    if (showPicker) {
        // The picker works in UTC midnight; only future days can be chosen
        val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val shownMonth: Long? = when {
            examDateMillis > 0L -> examDateMillis
            profileExamYear >= LocalDate.now().year ->
                LocalDate.of(profileExamYear, 8, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            else -> null
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = examDateMillis.takeIf { it > 0L },
            initialDisplayedMonthMillis = shownMonth,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis > todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showPicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------- shared bits ----------

@Composable
private fun HelpText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A tappable card with a radio button. The chosen one is highlighted. */
@Composable
private fun OptionCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) NenaPrimaryContainer else MaterialTheme.colorScheme.surface,
            // Dark text on the light highlight, so it stays readable in dark mode
            contentColor = if (selected) Color(0xFF1B1B1F) else MaterialTheme.colorScheme.onSurface
        ),
        border = if (selected) BorderStroke(1.5.dp, NenaPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.75f)
                )
            }
        }
    }
}