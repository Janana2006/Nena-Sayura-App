package lk.nenasayura.app.ui.student.timetable



import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.Subject
import java.time.LocalDate

/** A small form for a session the student adds themselves. It is placed after that day's other sessions. */
@Composable
fun AddSessionDialog(
    subjects: List<Subject>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (title: String, subject: Subject?, date: LocalDate, durationMinutes: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf<Subject?>(null) }
    var date by remember { mutableStateOf(if (initialDate.isBefore(LocalDate.now())) LocalDate.now() else initialDate) }
    var minutes by remember { mutableIntStateOf(45) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add my own session") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What will you study?") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Subject (optional)", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = subject == null,
                        onClick = { subject = null },
                        label = { Text("None") }
                    )
                    subjects.forEach { option ->
                        FilterChip(
                            selected = subject?.id == option.id,
                            onClick = { subject = option },
                            label = { Text(option.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Length", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 45, 60, 90).forEach { option ->
                        FilterChip(
                            selected = minutes == option,
                            onClick = { minutes = option },
                            label = { Text("$option min") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(formatDayLong(date))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title.trim(), subject, date, minutes) },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        PlannerDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                date = it
                showDatePicker = false
            }
        )
    }
}