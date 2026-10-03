package lk.nenasayura.app.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import lk.nenasayura.app.viewmodel.AuthUiState
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    onNavigate: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var showSent by remember { mutableStateOf(false) }
    var sentTo by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state is AuthUiState.Loading
    val primaryBlue = Color(0xFF2563EB)
    val lightBlueBg = Color(0xFFF0F5FF)

    // ViewModel eke state wenas unama
    LaunchedEffect(state) {
        when (val s = state) {
            is AuthUiState.Success -> {
                viewModel.resetState()
                sentTo = email.trim()
                showSent = true
                secondsLeft = 60
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Resend countdown
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: back arrow + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onNavigate("Login") },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reset Password",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Artistic Illustration Circle
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(lightBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(primaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showSent) Icons.Default.Email else Icons.Default.LockReset,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Card Container for Form/Status
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
                    if (!showSent) {
                        // ---------- Screen 1: email dala Send link ----------
                        Text(
                            text = "Forgot Password?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter your registered email address to receive a secure password reset link.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedLabelColor = primaryBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.sendResetEmail(email) },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                text = if (isLoading) "Please wait..." else "Send Reset Link",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // ---------- Send una passe: Check your email ----------
                        Text(
                            text = "Check Your Email",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "We have sent a password reset link to",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sentTo,
                            fontSize = 15.sp,
                            color = primaryBlue,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "If you don't see the email, please check your Spam or Junk folder.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = { onNavigate("Login") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Back to Login", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (secondsLeft > 0) {
                            Text(
                                text = "Resend link in ${secondsLeft}s",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            TextButton(
                                onClick = { viewModel.sendResetEmail(sentTo) },
                                enabled = !isLoading
                            ) {
                                Text(
                                    text = if (isLoading) "Sending..." else "Resend Link",
                                    color = primaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
