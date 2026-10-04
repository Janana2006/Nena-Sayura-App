package lk.nenasayura.app.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun RoleSelectScreen(
    modifier: Modifier,
    viewModel: AuthViewModel,
    onContinue: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val blue = Color(0xFF0066FF)
    val firstName = viewModel.getCurrentUser()?.name
        ?.substringBefore(" ")
        ?.takeIf { it.isNotBlank() }

    var role by remember { mutableStateOf<String?>(null) }
    var isFree by remember { mutableStateOf(true) }
    var feeText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    // Role eka thorana kan back eken yanna denna epa
    BackHandler(enabled = true) { }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (firstName != null) "Welcome, $firstName!" else "Welcome!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = blue
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tell us how you'll use Nena Sayura.",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        RoleCard(
            title = "I'm a Student",
            subtitle = "Practice papers, quizzes and track your study time",
            icon = Icons.Filled.School,
            selected = role == "student",
            onClick = { role = "student" }
        )
        RoleCard(
            title = "I'm a Teacher",
            subtitle = "Share notes and resources with students",
            icon = Icons.Filled.Person,
            selected = role == "teacher",
            onClick = { role = "teacher" }
        )

        // Teacher kenek nam witharak Free / Paid
        AnimatedVisibility(visible = role == "teacher") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your service", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlanCard(
                        title = "Free",
                        subtitle = "No charge for students",
                        selected = isFree,
                        onClick = { isFree = true },
                        modifier = Modifier.weight(1f)
                    )
                    PlanCard(
                        title = "Paid",
                        subtitle = "Students pay a fee",
                        selected = !isFree,
                        onClick = { isFree = false },
                        modifier = Modifier.weight(1f)
                    )
                }

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
                val chosen = role
                if (chosen != null) {
                    saving = true
                    viewModel.saveRole(chosen, isFree, feeText) { result ->
                        saving = false
                        result.fold(
                            onSuccess = { onContinue(chosen) },
                            onFailure = {
                                Toast.makeText(
                                    context,
                                    it.localizedMessage ?: "Something went wrong",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            },
            enabled = role != null && !saving,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = blue, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (saving) "Saving..." else "Continue", fontSize = 17.sp)
        }

        TextButton(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Logout") }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------- Student / Teacher card ----------
@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val blue = Color(0xFF0066FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(
                if (selected) Color(0xFFEAF1FF) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) blue else Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (selected) blue else Color(0xFFEAF1FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else blue
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        RadioButton(selected = selected, onClick = null)
    }
}

// ---------- Free / Paid card ----------
@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blue = Color(0xFF0066FF)

    Column(
        modifier = modifier
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) blue else Color.LightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = if (selected) blue else Color.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = Color.Gray)
    }
}