package lk.nenasayura.app.ui.student.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Change the login email. Needs the current password; the new email is verified with a link. */
@Composable
fun ChangeEmailScreen(
    currentEmail: String,
    saving: Boolean,
    onBack: () -> Unit,
    onSubmit: (currentPassword: String, newEmail: String) -> Unit
) {
    var newEmail by rememberSaveable { mutableStateOf("") }
    // Passwords are not saved with rememberSaveable on purpose
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SubPageHeader(
            title = "Email address",
            subtitle = "Change the email you log in with",
            onBack = onBack
        )

        InfoNote(
            "We will send a verification link to your new email. Your email changes only after " +
                    "you open that link. After that, sign in again with the new email."
        )

        FormCard {
            Text(
                text = "Current email",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentEmail,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(14.dp))
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Current password"
            )
            Spacer(modifier = Modifier.size(16.dp))
            PrimaryActionButton(
                text = "Send verification link",
                busy = saving,
                enabled = newEmail.isNotBlank() && password.isNotEmpty(),
                onClick = { onSubmit(password, newEmail.trim()) }
            )
        }
    }
}