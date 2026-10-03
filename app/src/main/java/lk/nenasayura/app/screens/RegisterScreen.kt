package lk.nenasayura.app.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    val primaryBlue = Color(0xFF2563EB)
    val lightBlueBg = Color(0xFFF0F5FF)

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF), Color(0xFFFFFFFF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Icon Badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(lightBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(primaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E3A8A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Join Nena Sayura today",
                fontSize = 15.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Student / Teacher toggle
            RoleSelector(role = role, onRoleChange = { role = it })
            Spacer(modifier = Modifier.height(20.dp))

            // Form Card Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = primaryBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryBlue) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = primaryBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = primaryBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = primaryBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Teacher kenek nam witharak penna
                    AnimatedVisibility(visible = role == "teacher") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                "Your Service Offering",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ServiceOption(
                                    title = "Free",
                                    subtitle = "No charge",
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
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedTextField(
                                        value = feeText,
                                        onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                                        label = { Text("Fee Amount") },
                                        placeholder = { Text("e.g. 1500") },
                                        prefix = { Text("Rs. ") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = primaryBlue,
                                            unfocusedBorderColor = Color(0xFFE2E8F0),
                                            focusedLabelColor = primaryBlue
                                        ),
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (isLoading) "Please wait..." else "Register",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account?",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
                TextButton(
                    onClick = { onNavigate("Login") },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Login",
                        color = primaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ---------- Student / Teacher toggle ----------
@Composable
private fun RoleSelector(role: String, onRoleChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E8F0), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        listOf("student" to "Student", "teacher" to "Teacher").forEach { (value, label) ->
            val selected = role == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) Color(0xFF2563EB) else Color.Transparent
                    )
                    .clickable { onRoleChange(value) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else Color(0xFF475569)
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
    val primaryBlue = Color(0xFF2563EB)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFF0F5FF) else Color(0xFFF8FAFC))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primaryBlue else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = if (selected) primaryBlue else Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            subtitle,
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )
    }
}
