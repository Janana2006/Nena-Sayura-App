package lk.nenasayura.app.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import lk.nenasayura.app.components.GoogleSignInButton
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.util.GoogleSignInHelper
import lk.nenasayura.app.viewmodel.AuthUiState
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    initialEmail: String = "",
    initialPassword: String = "",
    showGuestOption: Boolean = true,   // false nam "Continue as Guest" hide wenawa
    onNavigate: (String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var showPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state is AuthUiState.Loading

    LaunchedEffect(state) {
        when (val s = state) {
            is AuthUiState.Success -> {
                viewModel.resetState()
                onNavigate("Check")   // role / setup check karala hariyata route karanawa
            }
            is AuthUiState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Email + password dekama liyuwama lock eka "unlock" wenawa
    val unlocked = email.isNotBlank() && password.isNotBlank()
    val lockTint by animateColorAsState(
        targetValue = if (unlocked) NenaSecondary else NenaPrimary,
        label = "lockTint"
    )

    val fieldShape = RoundedCornerShape(14.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NenaPrimary,
        focusedLabelColor = NenaPrimary,
        cursorColor = NenaPrimary,
        focusedLeadingIconColor = NenaPrimary,
        unfocusedContainerColor = Color.White,
        focusedContainerColor = Color.White
    )

    Box(modifier = Modifier.fillMaxSize().background(NenaBackground)) {

        // Guest home eke wage gradient header (status bar eka yatathath yanawa)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
        )

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Lock badge: ring + white circle + lock icon
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = unlocked, label = "lockIcon") { open ->
                        Icon(
                            imageVector = if (open) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = lockTint,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Log in to unlock your A/L study tools",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = { onNavigate("Forgot") },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Forgot password?", color = NenaPrimary) }

                    // Login
                    Button(
                        onClick = { viewModel.login(email, password) },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NenaPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Text("Please wait...", fontSize = 16.sp)
                        } else {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(10.dp))
                            Text("Log in", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // "or" divider
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = NenaPrimaryContainer)
                        Text(
                            text = "  or  ",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = NenaPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Google login
                    GoogleSignInButton(
                        onClick = {
                            scope.launch {
                                GoogleSignInHelper.getIdToken(context).fold(
                                    onSuccess = { idToken -> viewModel.loginWithGoogle(idToken) },
                                    onFailure = { viewModel.showError(it.localizedMessage ?: "Google login cancelled") }
                                )
                            }
                        },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guest: Guest Home eken "Log in" click karala awoth meka hide wenawa
            if (showGuestOption) {
                TextButton(onClick = { onNavigate("Guest") }) {
                    Text("Continue as Guest", color = NenaPrimary, fontWeight = FontWeight.Medium)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account?", color = Color(0xFF6B7280))
                TextButton(onClick = { onNavigate("Register") }) {
                    Text("Register", color = NenaPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}