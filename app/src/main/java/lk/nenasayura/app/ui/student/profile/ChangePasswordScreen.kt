package lk.nenasayura.app.ui.student.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Change the password straight away (no email link): current password + new password. */
@Composable
fun ChangePasswordScreen(
    saving: Boolean,
    onBack: () -> Unit,
    onSubmit: (current: String, newPassword: String, confirm: String) -> Unit,
    onForgot: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubPageHeader(
            title = "Password",
            subtitle = "Choose a new password for your account",
            onBack = onBack
        )

        FormCard {
            PasswordField(
                value = current,
                onValueChange = { current = it },
                label = "Current password"
            )
            Spacer(modifier = Modifier.size(10.dp))
            PasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "New password (at least 6 characters)"
            )
            Spacer(modifier = Modifier.size(10.dp))
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirm new password"
            )
            Spacer(modifier = Modifier.size(16.dp))
            PrimaryActionButton(
                text = "Update password",
                busy = saving,
                enabled = current.isNotEmpty() && newPassword.isNotEmpty() && confirm.isNotEmpty(),
                onClick = { onSubmit(current, newPassword, confirm) }
            )
        }

        TextButton(onClick = onForgot) {
            Text("Forgot your current password? Email me a reset link")
        }
    }
}