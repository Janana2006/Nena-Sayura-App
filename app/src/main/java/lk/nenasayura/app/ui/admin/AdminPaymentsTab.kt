package lk.nenasayura.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.rs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

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
    onRefresh: () -> Unit,
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
            // This tab has no top bar (the overview draws its own header), so keep clear of the status bar
            modifier = modifier.statusBarsPadding()
        )
    } else {
        PaymentsOverview(
            state = state,
            onSaveCommission = onSaveCommission,
            onOpenTeacher = { openTeacherId = it },
            onRefresh = onRefresh,
            modifier = modifier
        )
    }
}

/** Time range used to filter the Students, Pay teachers and History lists. */
private enum class PayPeriod(val label: String) {
    ALL("All time"),
    WEEK("This week"),
    MONTH("This month"),
    YEAR("This year");

    /** First moment of the period (week starts on Monday). 0 = no limit. */
    fun startMillis(): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = when (this) {
            ALL -> return 0L
            WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            MONTH -> today.withDayOfMonth(1)
            YEAR -> today.withDayOfYear(1)
        }
        return start.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

@Composable
private fun PaymentsOverview(
    state: AdminUiState,
    onSaveCommission: (String, (String?) -> Unit) -> Unit,
    onOpenTeacher: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableIntStateOf(0) } // 0 students, 1 pay teachers, 2 history
    var periodIndex by rememberSaveable { mutableIntStateOf(0) } // 0 all, 1 week, 2 month, 3 year
    val period = PayPeriod.entries[periodIndex]
    var editing by remember { mutableStateOf(false) }
    // The payout whose details dialog is open (History tab)
    var openPayoutId by rememberSaveable { mutableStateOf<String?>(null) }
    val openPayout = state.payouts.firstOrNull { it.id == openPayoutId }

    // Same state, but only the payments and payouts inside the chosen period.
    // Every total (paid by students, commission, teachers' share, paid out) follows it.
    val view = remember(state, periodIndex) {
        if (period == PayPeriod.ALL) {
            state
        } else {
            val start = period.startMillis()
            state.copy(
                payments = state.payments.filter { it.createdAtMillis >= start },
                payouts = state.payouts.filter { it.createdAtMillis >= start }
            )
        }
    }

    // Teachers that still have money to pay (all time, so the amount to pay is always correct).
    // With a period chosen, only teachers who got student payments in that period are listed.
    val periodTeacherIds = view.paidPayments.map { it.teacherId }.toSet()
    val dueTeachers = state.teacherSummaries.filter {
        it.due > 0.0 && (period == PayPeriod.ALL || it.teacherId in periodTeacherIds)
    }

    // Same side margin as the Home tab; the header goes edge to edge
    val side = Modifier.padding(horizontal = 16.dp)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ---- Header (same look as the Home and Profile tabs) ----
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Payments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = money(state.stillToPay),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Still to pay to teachers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            }
        }

        // ---- Period filter: all time / this week / this month / this year ----
        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periodColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NenaSecondaryContainer,
                    selectedLabelColor = Color(0xFF004D40)
                )
                PayPeriod.entries.forEachIndexed { index, p ->
                    FilterChip(
                        selected = periodIndex == index,
                        onClick = { periodIndex = index },
                        colors = periodColors,
                        label = { Text(p.label) }
                    )
                }
            }
        }

        item {
            AdminCard(modifier = side) {
                MoneyRow("Total paid by students", money(view.gross))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow(
                    label = "Admin commission (${percentText(state.commissionPercent)}%)",
                    value = money(view.commission),
                    valueColor = NenaSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                MoneyRow("Teachers' share", money(view.teachersTotal))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow("Paid out", money(view.payoutsTotal), NenaSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change commission %")
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NenaPrimaryContainer,
                    selectedLabelColor = NenaPrimary
                )
                FilterChip(
                    selected = section == 0,
                    onClick = { section = 0 },
                    colors = chipColors,
                    label = { Text("Students (${view.payments.size})") }
                )
                FilterChip(
                    selected = section == 1,
                    onClick = { section = 1 },
                    colors = chipColors,
                    label = { Text("Pay teachers (${dueTeachers.size})") }
                )
                FilterChip(
                    selected = section == 2,
                    onClick = { section = 2 },
                    colors = chipColors,
                    label = { Text("History (${view.payouts.size})") }
                )
            }
        }

        when (section) {
            0 -> {
                if (view.payments.isEmpty()) {
                    item {
                        EmptyText(
                            if (period == PayPeriod.ALL) {
                                "No payments yet. When a student hires a teacher and pays, it will appear here."
                            } else {
                                "No student payments ${period.label.lowercase()}."
                            },
                            modifier = side
                        )
                    }
                } else {
                    items(view.payments, key = { it.id }) { payment ->
                        // Tap a payment to open that teacher (only teachers with a paid student can be paid)
                        val canOpen = state.teacherSummaries.any { it.teacherId == payment.teacherId }
                        Box(modifier = side) {
                            PaymentRow(
                                payment = payment,
                                onClick = if (canOpen) ({ onOpenTeacher(payment.teacherId) }) else null
                            )
                        }
                    }
                }
            }

            1 -> {
                if (dueTeachers.isEmpty()) {
                    item {
                        EmptyText(
                            if (period == PayPeriod.ALL) {
                                "Nothing to pay right now. Paid teachers are in the History tab."
                            } else {
                                "No teacher to pay for ${period.label.lowercase()}."
                            },
                            modifier = side
                        )
                    }
                } else {
                    items(dueTeachers, key = { it.teacherId }) { teacher ->
                        Box(modifier = side) {
                            TeacherDueCard(
                                summary = teacher,
                                // Share earned inside the chosen period (null = all time)
                                periodNet = if (period == PayPeriod.ALL) null else
                                    view.teacherSummaries.firstOrNull { it.teacherId == teacher.teacherId }?.net ?: 0.0,
                                periodLabel = period.label.lowercase(),
                                onClick = { onOpenTeacher(teacher.teacherId) }
                            )
                        }
                    }
                }
            }

            else -> {
                if (view.payouts.isEmpty()) {
                    item {
                        EmptyText(
                            if (period == PayPeriod.ALL) {
                                "You have not paid any teacher yet. Payments you confirm will be listed here."
                            } else {
                                "No teacher payouts ${period.label.lowercase()}."
                            },
                            modifier = side
                        )
                    }
                } else {
                    items(view.payouts, key = { it.id }) { payout ->
                        Box(modifier = side) {
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
private fun EmptyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
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
private fun TeacherDueCard(
    summary: TeacherSummary,
    periodNet: Double?,
    periodLabel: String,
    onClick: () -> Unit
) {
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
        if (periodNet != null) {
            MoneyRow("Earned $periodLabel", money(periodNet))
            Spacer(modifier = Modifier.height(4.dp))
        }
        MoneyRow("Teacher's share (all time)", money(summary.net))
        Spacer(modifier = Modifier.height(4.dp))
        MoneyRow("Paid out (all time)", money(summary.paidOut), NenaSecondary)
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