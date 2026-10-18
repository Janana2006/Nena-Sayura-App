package lk.nenasayura.app.ui.teacher.payments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.common.PayoutRow
import lk.nenasayura.app.ui.student.profile.FormCard
import lk.nenasayura.app.ui.student.profile.InfoNote
import lk.nenasayura.app.ui.student.profile.PrimaryActionButton
import lk.nenasayura.app.ui.student.profile.SubPageHeader
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.util.rs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Time range used to filter the payment history. */
private enum class HistoryPeriod(val label: String) {
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

/**
 * Payments tab of the teacher:
 *  - the bank account the admin sends the earnings to,
 *  - how much was earned, how much was already paid, and how much is still waiting,
 *  - the history of payments received, each with a link to its receipt.
 */
@Composable
fun TeacherPaymentsTab(
    profile: TeacherProfile,
    saving: Boolean,
    onSave: (bankName: String, accountNumber: String, branch: String, onSaved: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    payoutsViewModel: TeacherPayoutsViewModel = viewModel()
) {
    var editing by remember { mutableStateOf(false) }
    val payouts by payoutsViewModel.state.collectAsStateWithLifecycle()

    // Payment history filter: all time / this week / this month / this year
    var periodIndex by rememberSaveable { mutableIntStateOf(0) }
    val period = HistoryPeriod.entries[periodIndex]
    val shownPayouts = remember(payouts.payouts, periodIndex) {
        if (period == HistoryPeriod.ALL) {
            payouts.payouts
        } else {
            val start = period.startMillis()
            payouts.payouts.filter { it.createdAtMillis >= start }
        }
    }

    // Loads again every time the teacher opens this tab, so a new payment shows up
    LaunchedEffect(profile.uid) { payoutsViewModel.load(profile.uid) }

    // Back from the form returns to the bank card
    BackHandler(enabled = editing) { if (!saving) editing = false }

    if (editing) {
        BankForm(
            profile = profile,
            saving = saving,
            onCancel = { editing = false },
            onSave = { bank, account, branch -> onSave(bank, account, branch) { editing = false } },
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BankCard(profile)

            EarningsCard(state = payouts)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment history",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { payoutsViewModel.load(profile.uid) }) { Text("Refresh") }
            }

            // Period chips (only when there is something to filter)
            if (!payouts.loading && payouts.errorMessage == null && payouts.payouts.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NenaPrimaryContainer,
                        selectedLabelColor = NenaPrimary
                    )
                    HistoryPeriod.entries.forEachIndexed { index, p ->
                        FilterChip(
                            selected = periodIndex == index,
                            onClick = { periodIndex = index },
                            colors = chipColors,
                            label = { Text(p.label) }
                        )
                    }
                }
                Text(
                    text = "${period.label}: ${shownPayouts.size} payment${if (shownPayouts.size == 1) "" else "s"}" +
                            "  \u2022  ${rs(shownPayouts.sumOf { it.amount })}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NenaSecondary
                )
            }

            when {
                payouts.loading -> Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                payouts.errorMessage != null -> Text(
                    text = payouts.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )

                payouts.payouts.isEmpty() -> Text(
                    text = "No payments received yet. When the admin pays you, the time, " +
                            "amount and receipt will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                shownPayouts.isEmpty() -> Text(
                    text = "No payments received ${period.label.lowercase()}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> shownPayouts.forEach { payout ->
                    PayoutRow(payout = payout, showTeacher = false)
                }
            }

            InfoNote(
                if (profile.hasBankDetails) {
                    "The admin sends your earnings to this account. Keep the details correct."
                } else {
                    "Add your bank details so the admin can send you your earnings."
                }
            )

            PrimaryActionButton(
                text = if (profile.hasBankDetails) "Edit bank details" else "Add bank details",
                busy = false,
                enabled = true,
                onClick = { editing = true }
            )
        }
    }
}

/** Earned, already paid, still waiting. "Waiting" becomes Rs. 0.00 once everything is paid. */
@Composable
private fun EarningsCard(state: TeacherPayoutsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your earnings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            AmountRow("Total earned (after admin commission)", rs(state.earned))
            Spacer(modifier = Modifier.height(8.dp))
            AmountRow("Paid to your bank", rs(state.paidOut), NenaSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            AmountRow("Waiting to be paid", rs(state.pending), NenaPrimary, bold = true)
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, valueColor: Color = Color.Unspecified, bold: Boolean = false) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = style,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, style = style, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

/** Looks like a bank card: bank name, account number and branch on the app's gradient. */
@Composable
private fun BankCard(profile: TeacherProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Payout account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (profile.hasBankDetails) {
                Text(
                    text = profile.bankName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = profile.accountNumber.chunked(4).joinToString(" "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "BRANCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = profile.bankBranch,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "No bank details yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bank, account number and branch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/** The form: bank name, account number, branch. Fill all three, or leave all empty. */
@Composable
private fun BankForm(
    profile: TeacherProfile,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (bankName: String, accountNumber: String, branch: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var bankName by remember { mutableStateOf(profile.bankName) }
    var accountNumber by remember { mutableStateOf(profile.accountNumber) }
    var branch by remember { mutableStateOf(profile.bankBranch) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubPageHeader(
            title = "Bank details",
            subtitle = "The admin sends your earnings to this account",
            onBack = { if (!saving) onCancel() }
        )

        FormCard {
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank name") },
                singleLine = true,
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                label = { Text("Account number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it },
                label = { Text("Branch") },
                singleLine = true,
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        InfoNote("Fill all three fields. Leave all of them empty to remove your bank details.")

        PrimaryActionButton(
            text = "Save bank details",
            busy = saving,
            enabled = true,
            onClick = { onSave(bankName, accountNumber, branch) }
        )
        OutlinedButton(onClick = onCancel, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}