package lk.nenasayura.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    onNavigate: (String) -> Unit
) {
    val user = viewModel.getCurrentUser()
    val name = user?.name?.ifEmpty { user.email } ?: "Guest"

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = name, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                viewModel.logout()
                onNavigate("Login")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
        ) {
            Text(if (user == null) "Back to Login" else "Logout", fontSize = 18.sp)
        }
    }
}