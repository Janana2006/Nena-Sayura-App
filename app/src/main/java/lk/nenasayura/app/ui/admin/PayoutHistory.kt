package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.Payout
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.util.formatDateTime

/**
 * Admin > Payments > History.
 * One payment that was sent to a teacher, shown as a small table:
 * teacher name, email, stream, subject (+ amount and the time it was sent).
 * Tap it to open [PayoutDetailsDialog].
 *
 * [teacher] is the teacher's account (looked up from the users list by teacherId).
 */
@Composable
fun PayoutHistoryCard(payout: Payout, teacher: AppUser?, onClick: () -> Unit) {
    AdminCard(modifier = Modifier.clickable(onClick = onClick)) {
        TableRow("Teacher", payout.teacherName.ifBlank { teacher?.name.orEmpty() }.ifBlank { "-" }, bold = true)
        TableRow("Email", teacher?.email.orEmpty().ifBlank { "-" })
        TableRow("Stream", teacher?.streamName.orEmpty().ifBlank { "-" })
        TableRow("Subject", teacher?.subjectNames.orEmpty().joinToString(", ").ifBlank { "-" })
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(6.dp))
        TableRow("Sent", formatDateTime(payout.createdAtMillis).ifBlank { "-" })
        TableRow("Amount", money(payout.amount), valueColor = NenaPrimary, bold = true)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap to see the details",
            style = MaterialTheme.typography.labelMedium,
            color = NenaPrimary
        )
    }
}

@Composable
private fun TableRow(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Full details of one payout: who, how much, when, reference, who paid, and the receipt. */
@Composable
fun PayoutDetailsDialog(payout: Payout, teacher: AppUser?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment details") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TableRow("Teacher", payout.teacherName.ifBlank { teacher?.name.orEmpty() }.ifBlank { "-" }, bold = true)
                TableRow("Email", teacher?.email.orEmpty().ifBlank { "-" })
                TableRow("Stream", teacher?.streamName.orEmpty().ifBlank { "-" })
                TableRow("Subject", teacher?.subjectNames.orEmpty().joinToString(", ").ifBlank { "-" })
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                TableRow("Amount", money(payout.amount), valueColor = NenaPrimary, bold = true)
                TableRow("Sent on", formatDateTime(payout.createdAtMillis).ifBlank { "-" })
                TableRow("Reference", payout.reference.ifBlank { "-" })
                TableRow("Paid by", payout.paidByName.ifBlank { "-" })
                if (teacher != null && teacher.bankName.isNotBlank()) {
                    TableRow("Bank", "${teacher.bankName}, ${teacher.bankBranch}")
                    TableRow("Account", teacher.accountNumber)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (payout.hasReceipt) {
                    Button(
                        onClick = { FileActions.openFile(context, payout.receiptUrl, "Payment receipt") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View receipt")
                    }
                } else {
                    Text(
                        text = "No receipt was added for this payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}