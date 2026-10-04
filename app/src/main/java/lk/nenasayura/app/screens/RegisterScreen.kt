package lk.nenasayura.app.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaAccentContainer
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
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
    var showPassword by remember { mutableStateOf(false) }
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

    val fieldShape = RoundedCornerShape(14.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NenaPrimary,
        focusedLabelColor = NenaPrimary,
        cursorColor = NenaPrimary,
        focusedLeadingIconColor = NenaPrimary,
        unfocusedContainerColor = Color.White,
        focusedContainerColor = Color.White
    )
    val passwordTransformation: VisualTransformation =
        if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
    val eyeButton: @Composable () -> Unit = {
        IconButton(onClick = { showPassword = !showPassword }) {
            Icon(
                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Show password"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NenaBackground)) {

        // Guest home eke wage gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
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
            Spacer(modifier = Modifier.height(24.dp))

            // Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (role == "teacher") Icons.Default.Person else Icons.Default.School,
                        contentDescription = null,
                        tint = NenaPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Create account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (role == "teacher") "Share your classes with A/L students"
                else "Unlock papers, quizzes and teachers",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("I am a", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.height(8.dp))
                    RoleSelector(role = role, onRoleChange = { role = it })
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Full name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true, shape = fieldShape, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true, shape = fieldShape, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = eyeButton,
                        visualTransformation = passwordTransformation,
                        singleLine = true, shape = fieldShape, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirm, onValueChange = { confirm = it },
                        label = { Text("Confirm password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = passwordTransformation,
                        singleLine = true, shape = fieldShape, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
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
                                        singleLine = true, shape = fieldShape, colors = fieldColors,
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
                            Text("Create account", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account?", color = Color(0xFF6B7280))
                TextButton(onClick = { onNavigate("Login") }) {
                    Text("Login", color = NenaPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------- Student / Teacher toggle ----------
@Composable
private fun RoleSelector(role: String, onRoleChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NenaPrimaryContainer, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        RoleChip(
            label = "Student",
            icon = Icons.Default.School,
            selected = role == "student",
            onClick = { onRoleChange("student") },
            modifier = Modifier.weight(1f)
        )
        RoleChip(
            label = "Teacher",
            icon = Icons.Default.Person,
            selected = role == "teacher",
            onClick = { onRoleChange("teacher") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RoleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(if (selected) NenaPrimary else Color.Transparent, label = "chipBg")
    val fg by animateColorAsState(if (selected) Color.White else NenaPrimary, label = "chipFg")

    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = label, fontWeight = FontWeight.Medium, color = fg)
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
    val accent = if (title == "Paid") NenaAccent else NenaSecondary
    val container = if (title == "Paid") NenaAccentContainer else NenaPrimaryContainer

    Column(
        modifier = modifier
            .background(if (selected) container else Color.White, RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else Color(0xFFE0E3EB),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF1B1B1F) else Color(0xFF6B7280),
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF6B7280))
    }
}