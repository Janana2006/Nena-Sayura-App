package lk.nenasayura.app.ui.guest



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.core.session.DevSessionProvider
import lk.nenasayura.app.ui.common.NenaTopBar

/**
 * TEMPORARY stand-in for the real Login screen (built by the authentication teammate).
 * The dev button only changes the dev session so Student screens can be tested.
 */
@Composable
fun LoginPlaceholderScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { NenaTopBar(title = "Log in", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Login screen", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The real login and registration screens are built by the " +
                        "authentication teammate and will replace this placeholder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = { DevSessionProvider.signInAsTestStudent() }) {
                Text("DEV ONLY: sign in as test student")
            }
        }
    }
}