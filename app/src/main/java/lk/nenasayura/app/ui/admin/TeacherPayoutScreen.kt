package lk.nenasayura.app.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale
import lk.nenasayura.app.data.model.Payment
import lk.nenasayura.app.data.model.Payout
import lk.nenasayura.app.ui.common.PayoutRow
import lk.nenasayura.app.ui.student.profile.SubPageHeader
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.util.rs

/**
 * Admin > Payments > one teacher.
 * Shows what the teacher earned and what is still to pay, the teacher's bank details,
 * the students who paid, and the form to record a payment: amount, bank reference, receipt,
 * then "Confirm payment". Confirming saves the payout, notifies the teacher and
 * brings "To pay now" down (to zero when everything is paid).
 */
@Composable
fun TeacherPayoutScreen(
    summary: TeacherSummary,
    payments: List<Payment>,
    payouts: List<Payout>,
    busy: Boolean,
    onBack: () -> Unit,
    onPay: (amount: String, reference: String, receipt: PickedFile?, onResult: (String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SubPageHeader(
                title = summary.teacherName,
                subtitle = "${summary.students} student${if (summary.students == 1) "" else "s"} paid",
                onBack = onBack
            )
        }

        // ---- How much ----
        item {
            AdminCard {
                MoneyRow("Total paid by students", rs(summary.gross))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow("Admin commission", rs(summary.commission), NenaSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                MoneyRow("Teacher's share", rs(summary.net))
                Spacer(modifier = Modifier.height(8.dp))
                MoneyRow("Already paid out", rs(summary.paidOut), NenaSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                MoneyRow("To pay now", rs(summary.due), NenaPrimary, bold = true)
            }
        }

        // ---- Where to send it ----
        item {
            AdminCard {
                Text(
                    text = "Bank details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (summary.hasBankDetails) {
                    BankLine("Bank", summary.bankName)
                    BankLine("Account number", summary.accountNumber)
                    BankLine("Branch", summary.bankBranch)
                } else {
                    Text(
                        text = "This teacher has not added bank details yet. Ask them to add " +
                                "them in the Payments tab of their app. You can pay once they do.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Pay ----
        item {
            when {
                summary.due <= 0.0 -> AdminCard {
                    Text(
                        text = "All paid",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NenaSecondary
                    )
                    Text(
                        text = "Nothing is waiting to be paid to this teacher.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                summary.hasBankDetails -> PayForm(summary = summary, busy = busy, onPay = onPay)

                else -> Unit
            }
        }

        // ---- Students who paid ----
        item { SectionTitle("Students who paid") }
        if (payments.isEmpty()) {
            item {
                Text(
                    text = "No student payments yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(payments, key = { it.id }) { payment -> PaymentRow(payment) }
        }

        // ---- Payments made to this teacher ----
        item { SectionTitle("Payment history") }
        if (payouts.isEmpty()) {
            item {
                Text(
                    text = "You have not paid this teacher yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(payouts, key = { it.id }) { payout -> PayoutRow(payout = payout, showTeacher = false) }
        }
    }
}

@Composable
private fun PayForm(
    summary: TeacherSummary,
    busy: Boolean,
    onPay: (amount: String, reference: String, receipt: PickedFile?, onResult: (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current

    // The amount starts as everything that is still to pay
    var amount by remember(summary.due) { mutableStateOf(String.format(Locale.US, "%.2f", summary.due)) }
    var reference by remember { mutableStateOf("") }
    var receipt by remember { mutableStateOf<PickedFile?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val (name, _) = context.queryFileInfo(uri)
            receipt = PickedFile(uri, name)
            error = null
        }
    }

    AdminCard {
        Text(
            text = "Pay the teacher",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Send the money to the bank account above. Then add the bank receipt and confirm.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Amount paid (Rs.)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = reference,
            onValueChange = { reference = it },
            label = { Text("Bank reference no. (optional)") },
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = { picker.launch(arrayOf("image/*", "application/pdf")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = receipt?.name ?: "Add receipt (photo or PDF)",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = {
                val value = amount.toDoubleOrNull()
                when {
                    value == null || value <= 0.0 -> error = "Enter the amount you paid."
                    receipt == null -> error = "Add the payment receipt first."
                    else -> {
                        error = null
                        confirming = true
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (busy) "Saving..." else "Confirm payment")
        }
    }

    if (confirming) {
        val value = amount.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Confirm payment") },
            text = {
                Text(
                    "You paid ${rs(value)} to ${summary.teacherName}?\n\n" +
                            "The teacher gets a notification with the receipt. " +
                            "This cannot be changed later."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onPay(amount, reference, receipt) { err ->
                        if (err != null) {
                            error = err
                        } else {
                            receipt = null
                            reference = ""
                        }
                    }
                }) { Text("Yes, confirm") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } }
        )
    }
}

@Composable
internal fun BankLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}