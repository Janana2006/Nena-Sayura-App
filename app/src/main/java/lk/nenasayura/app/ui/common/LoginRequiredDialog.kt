package lk.nenasayura.app.ui.common



import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Shown when a guest taps a feature that needs a student account. */
@Composable
fun LoginRequiredDialog(
    featureName: String,
    onDismiss: () -> Unit,
    onLogin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
        title = { Text("Login required") },
        text = {
            Text(
                "Log in as a student to use \"$featureName\". " +
                        "Papers, marking schemes, quizzes and guide books stay free without an account."
            )
        },
        confirmButton = { Button(onClick = onLogin) { Text("Log in") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}