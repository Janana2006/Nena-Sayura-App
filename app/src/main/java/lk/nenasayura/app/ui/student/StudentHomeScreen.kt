package lk.nenasayura.app.ui.student



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.core.session.UserSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.ui.common.NenaTopBar

/** Temporary Student Home. The full version is built in Task 7. */
@Composable
fun StudentHomeScreen(onSignOut: () -> Unit) {
    val session by currentSession()
    val name = (session as? UserSession.LoggedIn)?.name ?: "Student"

    Scaffold(
        topBar = { NenaTopBar(title = "Student Home") },
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
            Text(text = "Welcome, $name", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The full Student Home is built in Task 7.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSignOut) { Text("Sign out") }
        }
    }
}