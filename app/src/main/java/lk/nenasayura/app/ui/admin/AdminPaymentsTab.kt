package lk.nenasayura.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import lk.nenasayura.app.util.rs

/**
 * Payments tab of the admin.
 *  - Student payments: who paid which teacher (tap one to open that teacher).
 *  - Pay teachers: every teacher with what is still to pay. Tap one to see the bank details,
 *    add the receipt and confirm the payment.
 *  - History: every payment made to a teacher, with a link to its receipt.
 * The commission % can be changed here.
 */
@Composable
fun AdminPaymentsTab(
    state: AdminUiState,
    onSaveCommission: (String, (String?) -> Unit) -> Unit,
    onPayTeacher: (
        teacherId: String,
        amount: String,
        reference: String,
        receipt: PickedFile?,
        onResult: (String?) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var openTeacherId by rememberSaveable { mutableStateOf<String?>(null) }
    val summary = state.teacherSummaries.firstOrNull { it.teacherId == openTeacherId }

    // Back from a teacher returns to the list
    BackHandler(enabled = summary != null) { openTeacherId = null }

    if (summary != null) {
        TeacherPayoutScreen(
            summary = summary,
            payments = state.paidPayments.filter { it.teacherId == summary.teacherId },
            payouts = state.payouts.filter { it.teacherId == summary.teacherId },
            busy = state.busy,
            onBack = { openTeacherId = null },
            onPay = { amount, reference, receipt, onResult ->
                onPayTeacher(summary.teacherId, amount, reference, receipt, onResult)
            },
            modifier = modifier
        )
    } else {
        PaymentsOverview(
            state = state,
            onSaveCommission = onSaveCommission,
            onOpenTeacher = { openTeacherId = it },
            modifier = modifier
        )
    }
}

@Composable
private fun PaymentsOverview(
    state: AdminUiState,
    onSaveCommission: (String, (String?) -> Unit) -> Unit,
    onOpenTeacher: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableIntStateOf(0) } // 0 students, 1 pay teachers, 2 history
    var editing by remember { mutableStateOf(false) }
    // The payout whose details dialog is open (History tab)
    var openPayoutId by rememberSaveable { mutableStateOf<String?>(null) }
    val openPayout = state.payouts.firstOrNull { it.id == openPayoutId }

    // Teachers that still have money to pay. Fully paid teachers move to History.
    val dueTeachers = state.teacherSummaries.filter { it.due > 0.0 }

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
                MoneyRow("Teachers' share", money(state.teachersTotal))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow("Paid out so far", money(state.payoutsTotal), NenaSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                MoneyRow("Still to pay", money(state.stillToPay), NenaPrimary, bold = true)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change commission %")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = section == 0,
                    onClick = { section = 0 },
                    label = { Text("Students (${state.payments.size})") }
                )
                FilterChip(
                    selected = section == 1,
                    onClick = { section = 1 },
                    label = { Text("Pay teachers (${dueTeachers.size})") }
                )
                FilterChip(
                    selected = section == 2,
                    onClick = { section = 2 },
                    label = { Text("History (${state.payouts.size})") }
                )
            }
        }

        when (section) {
            0 -> {
                if (state.payments.isEmpty()) {
                    item {
                        EmptyText(
                            "No payments yet. When a student hires a teacher and pays, it will appear here."
                        )
                    }
                } else {
                    items(state.payments, key = { it.id }) { payment ->
                        // Tap a payment to open that teacher (only teachers with a paid student can be paid)
                        val canOpen = state.teacherSummaries.any { it.teacherId == payment.teacherId }
                        PaymentRow(
                            payment = payment,
                            onClick = if (canOpen) ({ onOpenTeacher(payment.teacherId) }) else null
                        )
                    }
                }
            }

            1 -> {
                if (dueTeachers.isEmpty()) {
                    item { EmptyText("Nothing to pay right now. Paid teachers are in the History tab.") }
                } else {
                    items(dueTeachers, key = { it.teacherId }) { teacher ->
                        TeacherDueCard(teacher, onClick = { onOpenTeacher(teacher.teacherId) })
                    }
                }
            }

            else -> {
                if (state.payouts.isEmpty()) {
                    item { EmptyText("You have not paid any teacher yet. Payments you confirm will be listed here.") }
                } else {
                    items(state.payouts, key = { it.id }) { payout ->
                        PayoutHistoryCard(
                            payout = payout,
                            teacher = state.users.firstOrNull { it.uid == payout.teacherId },
                            onClick = { openPayoutId = payout.id }
                        )
                    }
                }
            }
        }
    }

    if (openPayout != null) {
        PayoutDetailsDialog(
            payout = openPayout,
            teacher = state.users.firstOrNull { it.uid == openPayout.teacherId },
            onDismiss = { openPayoutId = null }
        )
    }

    if (editing) {
        CommissionDialog(
            current = state.commissionPercent,
            onSave = onSaveCommission,
            onDismiss = { editing = false }
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** One payment of a student. Pass [onClick] to open the teacher it was paid to. */
@Composable
fun PaymentRow(payment: Payment, onClick: (() -> Unit)? = null) {
    AdminCard(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
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
                    color = if (onClick != null) NenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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

/** A teacher in the "Pay teachers" list: share, paid out, and what is still to pay. */
@Composable
private fun TeacherDueCard(summary: TeacherSummary, onClick: () -> Unit) {
    AdminCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
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
            }
            if (summary.due > 0.0) {
                Pill("${money(summary.due)} to pay", NenaAccentContainer, Color(0xFF6B4A00))
            } else {
                Pill("All paid", NenaSecondaryContainer, Color(0xFF004D40))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        MoneyRow("Teacher's share", money(summary.net))
        Spacer(modifier = Modifier.height(4.dp))
        MoneyRow("Paid out", money(summary.paidOut), NenaSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (summary.due > 0.0) "Tap to pay" else "Tap to see the details",
            style = MaterialTheme.typography.labelMedium,
            color = NenaPrimary
        )
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