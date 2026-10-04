package lk.nenasayura.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    code: String,
    onNavigate: (String) -> Unit
) {
    var checking by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf<String?>(null) }   // null = link eka waradi
    var done by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val primaryBlue = Color(0xFF2563EB)
    val lightBlueBg = Color(0xFFF0F5FF)

    // Screen eka open wenakota link eke code eka hari da balanawa
    LaunchedEffect(code) {
        viewModel.verifyResetCode(code) { result ->
            email = result.getOrNull()
            checking = false
        }
    }

    val visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
    val eyeIcon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility

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
            // Header Illustration Circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(lightBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(primaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nena Sayura",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E3A8A)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        // 1. Link eka check karanawa
                        checking -> {
                            Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = primaryBlue)
                            }
                        }

                        // 3. Password eka wenas una
                        done -> {
                            Text("Password Updated", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "You can now log in with your new password securely.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onNavigate("Login") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White)
                            ) { Text("Go to Login", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                        }

                        // Link eka expire wela / waradi
                        email == null -> {
                            Text("Link Expired or Invalid", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Reset links expire and work only once. Please request a new one from Forgot Password.",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onNavigate("Forgot") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White)
                            ) { Text("Request New Link", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { onNavigate("Login") }) {
                                Text("Back to Login", color = primaryBlue, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // 2. New password + Confirm password form eka
                        else -> {
                            Text("Choose New Password", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(email.orEmpty(), fontSize = 14.sp, color = primaryBlue, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; error = "" },
                                label = { Text("New Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(eyeIcon, contentDescription = "Show password", tint = Color(0xFF94A3B8))
                                    }
                                },
                                visualTransformation = visual,
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
                                onValueChange = { confirm = it; error = "" },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryBlue) },
                                visualTransformation = visual,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedLabelColor = primaryBlue
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (error.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(error, color = Color(0xFFDC2626), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    saving = true
                                    viewModel.confirmPasswordReset(code, password, confirm) { result ->
                                        saving = false
                                        result.fold(
                                            onSuccess = { done = true },
                                            onFailure = { error = it.message ?: "Something went wrong" }
                                        )
                                    }
                                },
                                enabled = !saving,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White)
                            ) {
                                Text(if (saving) "Saving..." else "Save New Password", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
