package lk.nenasayura.app.ui.teacher.payments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.student.profile.FormCard
import lk.nenasayura.app.ui.student.profile.InfoNote
import lk.nenasayura.app.ui.student.profile.PrimaryActionButton
import lk.nenasayura.app.ui.student.profile.SubPageHeader
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSecondary

/**
 * Payments tab of the teacher: the bank account the admin sends the earnings to.
 * The admin sees the same details in the Admin > Payments tab.
 */
@Composable
fun TeacherPaymentsTab(
    profile: TeacherProfile,
    saving: Boolean,
    onSave: (bankName: String, accountNumber: String, branch: String, onSaved: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }

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