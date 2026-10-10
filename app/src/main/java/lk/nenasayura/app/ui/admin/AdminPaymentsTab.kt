package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

/**
 * Payments tab: which student paid which teacher, how much, and what the admin
 * and the teacher each get. The commission % can be changed here.
 */
@Composable
fun AdminPaymentsTab(
    state: AdminUiState,
    onSaveCommission: (String, (String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var byTeacher by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AdminCard {
                MoneyRow("Total paid by students", money(state.gross))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow(
                    label = "Admin commission (${percentText(state.commissionPercent)}%)",
                    value = money(state.commission),
                    valueColor = NenaSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                MoneyRow("Paid out to teachers", money(state.teachersTotal), bold = true)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change commission %")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !byTeacher,
                    onClick = { byTeacher = false },
                    label = { Text("All payments (${state.payments.size})") }
                )
                FilterChip(
                    selected = byTeacher,
                    onClick = { byTeacher = true },
                    label = { Text("By teacher (${state.teacherSummaries.size})") }
                )
            }
        }

        if (state.payments.isEmpty()) {
            item {
                Text(
                    text = "No payments yet. When a student hires a teacher and pays, " +
                            "it will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (!byTeacher) {
            items(state.payments, key = { it.id }) { payment -> PaymentRow(payment) }
        } else {
            items(state.teacherSummaries, key = { it.teacherId + it.teacherName }) { summary ->
                TeacherSummaryCard(summary)
            }
        }
    }

    if (editing) {
        CommissionDialog(
            current = state.commissionPercent,
            onSave = onSaveCommission,
            onDismiss = { editing = false }
        )
    }
}

/** One payment: "Student hired Teacher", subject, date, amount and status. */
@Composable
fun PaymentRow(payment: Payment) {
    AdminCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.studentName.ifBlank { "Student" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "hired ${payment.teacherName.ifBlank { "a teacher" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val sub = listOf(payment.subjectName, formatDate(payment.createdAtMillis))
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = money(payment.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NenaPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    payment.isPaid -> Pill("Paid", NenaSecondaryContainer, Color(0xFF004D40))
                    payment.isPending -> Pill("Pending", NenaAccentContainer, Color(0xFF6B4A00))
                    else -> Pill(payment.status.replaceFirstChar { it.uppercase() }, Color(0xFFFFDAD6), Color(0xFF93000A))
                }
            }
        }
    }
}

@Composable
private fun TeacherSummaryCard(summary: TeacherSummary) {
    AdminCard {
        Text(
            text = summary.teacherName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "${summary.students} student${if (summary.students == 1) "" else "s"} paid",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        MoneyRow("Total paid by students", money(summary.gross))
        Spacer(modifier = Modifier.height(6.dp))
        MoneyRow("Admin commission", money(summary.commission), NenaSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))
        MoneyRow("Teacher receives", money(summary.net), NenaPrimary, bold = true)
    }
}

@Composable
private fun CommissionDialog(
    current: Double,
    onSave: (String, (String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(percentText(current)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin commission") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "The percentage the app keeps from every payment made to a teacher.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Commission (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = null
                onSave(text) { err -> if (err == null) onDismiss() else error = err }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}