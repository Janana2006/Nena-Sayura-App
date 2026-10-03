package lk.nenasayura.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun CheckScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    onNavigate: (String) -> Unit
) {
    var error by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        error = false
        viewModel.checkDestination { result ->
            result.fold(
                onSuccess = { route -> onNavigate(route) },
                onFailure = { error = true }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (error) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Couldn't load your profile. Check your internet connection.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { attempt++ }) { Text("Retry") }
                TextButton(onClick = {
                    viewModel.logout()
                    onNavigate("Login")
                }) { Text("Logout") }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}