package lk.nenasayura.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.data.model.User
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun TeacherHomeScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    onNavigate: (String) -> Unit
) {
    var profile by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile { profile = it.getOrNull() }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Teacher Home", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
        Spacer(modifier = Modifier.height(12.dp))

        profile?.let { p ->
            Text(p.name, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(p.streamName, fontSize = 16.sp, color = Color.Gray)
            Text(
                p.subjectNames.joinToString(", "),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                viewModel.logout()
                onNavigate("Login")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
        ) { Text("Logout", fontSize = 18.sp) }
    }
}