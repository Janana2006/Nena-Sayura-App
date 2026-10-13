package lk.nenasayura.app.ui.admin

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary

/**
 * Profile tab of the Admin: a friendly header, the platform at a glance, the account
 * details (edit name, change password, sign out) and the admin team with Add staff.
 */
@Composable
fun AdminProfileTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    profileVm: AdminProfileViewModel = viewModel()
) {
    val profile by profileVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditName by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showAddStaff by remember { mutableStateOf(false) }

    val displayName = profile.name.ifBlank { "Admin" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(name = displayName, email = profile.email)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Platform at a glance ----
            SectionTitle("Platform at a glance")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Students", state.students.size.toString(), Modifier.weight(1f))
                StatCard("Teachers", state.teachers.size.toString(), Modifier.weight(1f))
                StatCard("Papers", state.papers.size.toString(), Modifier.weight(1f))
            }

            // ---- Account details ----
            SectionTitle("Account", modifier = Modifier.padding(top = 8.dp))
            AdminCard {
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Name",
                    value = displayName,
                    trailing = { TextButton(onClick = { showEditName = true }) { Text("Edit") } }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(Icons.Filled.Email, "Email", profile.email.ifBlank { "Not available" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(Icons.Filled.AdminPanelSettings, "Role", "Administrator")
                if (profile.memberSince > 0L) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(Icons.Filled.DateRange, "Admin since", monthYear(profile.memberSince))
                }
            }

            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change password")
            }
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign out")
            }

            // ---- Admin team ----
            SectionTitle("Admin team", modifier = Modifier.padding(top = 8.dp))
            Text(
                text = "Staff are other admins. They log in from the normal Login screen " +
                        "with the email and password you set here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { showAddStaff = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add staff member")
            }

            state.staff.forEach { member ->
                AdminCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(name = member.name, size = 44)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name.ifBlank { "No name" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = member.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (member.email.equals(profile.email, ignoreCase = true)) {
                            Pill("You", NenaPrimaryContainer, NenaPrimary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showEditName) {
        EditNameDialog(
            current = displayName,
            busy = profile.busy,
            onSave = { name, done -> profileVm.updateName(name, done) },
            onDone = {
                Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                showEditName = false
            },
            onDismiss = { showEditName = false }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Change password") },
            text = {
                Text("We will email a link to ${profile.email} so you can choose a new password.")
            },
            confirmButton = {
                TextButton(
                    enabled = !profile.busy,
                    onClick = {
                        profileVm.sendPasswordReset { err ->
                            Toast.makeText(
                                context,
                                err ?: "Reset link sent. Check your email.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        showReset = false
                    }
                ) { Text("Send link") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }

    if (showAddStaff) {
        AddStaffDialog(
            busy = state.busy,
            viewModel = viewModel,
            onDismiss = { showAddStaff = false }
        )
    }
}

/** Gradient header: time-of-day greeting, a big avatar, the name, the email and the role. */
@Composable
private fun ProfileHeader(name: String, email: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with a soft ring around it
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "A",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = NenaPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = greeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Administrator",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/** A round avatar with the first letter of a name. */
@Composable
private fun InitialAvatar(name: String, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/** One account line: a round icon, a small label and the value. */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(NenaPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NenaPrimary)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun EditNameDialog(
    current: String,
    busy: Boolean,
    onSave: (String, (String?) -> Unit) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Edit name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your name") },
                singleLine = true,
                enabled = !busy,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    error = null
                    onSave(name) { err -> if (err == null) onDone() else error = err }
                }
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

@Composable
private fun AddStaffDialog(busy: Boolean, viewModel: AdminViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Add staff member") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 6 characters)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    error = null
                    viewModel.addStaff(name, email, password) { err ->
                        if (err == null) onDismiss() else error = err
                    }
                }
            ) { Text(if (busy) "Creating…" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun monthYear(millis: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(millis))