package lk.nenasayura.app.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.viewmodel.AuthUiState
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    onNavigate: (String) -> Unit,
    onRegistered: (String, String) -> Unit
) {
    var role by remember { mutableStateOf("student") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isFree by remember { mutableStateOf(true) }
    var feeText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state is AuthUiState.Loading

    LaunchedEffect(state) {
        when (val s = state) {
            is AuthUiState.Success -> {
                viewModel.resetState()
                viewModel.logout()   // auto login wela thiyena eka ain karanna
                Toast.makeText(context, "Account created. Please login", Toast.LENGTH_SHORT).show()
                onRegistered(email.trim(), password)
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create Account", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
        Spacer(modifier = Modifier.height(24.dp))

        // Student / Teacher toggle
        RoleSelector(role = role, onRoleChange = { role = it })
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Full name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { confirm = it },
            label = { Text("Confirm password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        // Teacher kenek nam witharak penna
        AnimatedVisibility(visible = role == "teacher") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Your service", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ServiceOption(
                        title = "Free",
                        subtitle = "No charge for students",
                        selected = isFree,
                        onClick = { isFree = true },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceOption(
                        title = "Paid",
                        subtitle = "Students pay a fee",
                        selected = !isFree,
                        onClick = { isFree = false },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Paid select kalama fee field eka
                AnimatedVisibility(visible = !isFree) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                            label = { Text("Fee amount") },
                            placeholder = { Text("e.g. 1500") },
                            prefix = { Text("Rs. ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.register(name, email, password, confirm, role, isFree, feeText)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue, contentColor = Color.White)
        ) {
            Text(if (isLoading) "Please wait..." else "Register", fontSize = 18.sp)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?")
            TextButton(onClick = { onNavigate("Login") }) { Text("Login") }
        }
    }
}

// ---------- Student / Teacher toggle ----------
@Composable
private fun RoleSelector(role: String, onRoleChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEAF1FF), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        listOf("student" to "Student", "teacher" to "Teacher").forEach { (value, label) ->
            val selected = role == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) Color.Blue else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                    .clickable { onRoleChange(value) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White else Color.Blue
                )
            }
        }
    }
}

// ---------- Free / Paid card ----------
@Composable
private fun ServiceOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.Blue else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = if (selected) Color.Blue else Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = Color.Gray)
    }
}