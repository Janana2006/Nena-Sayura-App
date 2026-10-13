package lk.nenasayura.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import lk.nenasayura.app.data.model.AdminPermission
import lk.nenasayura.app.data.model.AppUser
import lk.nenasayura.app.data.model.StaffRole
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

/**
 * Team section of the Admin profile. Only shown to admins with the "Manage Staff" access,
 * so it lists the staff and lets them add a new member or edit someone's access.
 * It sits inside the profile's scrolling column, so it must NOT scroll by itself.
 */
@Composable
fun AdminStaffTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AppUser?>(null) }
    val myEmail = remember { FirebaseAuth.getInstance().currentUser?.email.orEmpty() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Your team", modifier = Modifier.weight(1f))
            Pill("${state.staff.size} admins", NenaPrimaryContainer, NenaPrimary)
        }
        Text(
            text = "Other admins log in from the normal Login screen with the email and password you set. " +
                    "They only see the parts you tick for them.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = { showAdd = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add staff member")
        }

        state.staff.forEach { member ->
            val isMe = member.email.equals(myEmail, ignoreCase = true)
            AdminCard {
                Row(verticalAlignment = Alignment.Top) {
                    InitialsAvatar(
                        initials = initialsOf(member.name, member.email),
                        size = 44.dp,
                        tint = if (isMe) NenaPrimary else NenaSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.name.ifBlank { "No name" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isMe) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Pill("You", NenaPrimaryContainer, NenaPrimary)
                            }
                        }
                        Text(
                            text = member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Pill(member.staffRoleLabel, NenaSecondaryContainer, NenaSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = accessSummary(member),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isMe) {
                        IconButton(onClick = { editing = member }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit access", tint = NenaPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddStaffDialog(
            busy = state.busy,
            allowed = state.access,
            viewModel = viewModel,
            onDismiss = { showAdd = false }
        )
    }

    editing?.let { member ->
        EditAccessDialog(
            member = member,
            busy = state.busy,
            allowed = state.access,
            viewModel = viewModel,
            onDismiss = { editing = null }
        )
    }
}

private fun accessSummary(member: AppUser): String {
    val access = member.access
    return when {
        access.size == AdminPermission.entries.size -> "Access: Everything"
        access.isEmpty() -> "Access: Home only"
        else -> "Access: " + AdminPermission.entries.filter { it in access }.joinToString(", ") { it.label }
    }
}

// ------------------------------- Add staff -------------------------------

@Composable
private fun AddStaffDialog(
    busy: Boolean,
    allowed: Set<AdminPermission>,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.FULL_ADMIN) }
    var ticked by remember { mutableStateOf(StaffRole.FULL_ADMIN.permissions.intersect(allowed)) }
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

                RoleAndAccessFields(
                    allowed = allowed,
                    role = role,
                    ticked = ticked,
                    enabled = !busy,
                    onChange = { newRole, newTicked ->
                        role = newRole
                        ticked = newTicked
                    }
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
                    viewModel.addStaff(name, email, password, role, ticked) { err ->
                        if (err == null) onDismiss() else error = err
                    }
                }
            ) { Text(if (busy) "Creating…" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

// ------------------------------- Edit access -------------------------------

@Composable
private fun EditAccessDialog(
    member: AppUser,
    busy: Boolean,
    allowed: Set<AdminPermission>,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var role by remember { mutableStateOf(StaffRole.fromKey(member.staffRole) ?: StaffRole.FULL_ADMIN) }
    var ticked by remember { mutableStateOf(member.access.intersect(allowed)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Edit access") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = member.name.ifBlank { member.email },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                RoleAndAccessFields(
                    allowed = allowed,
                    role = role,
                    ticked = ticked,
                    enabled = !busy,
                    onChange = { newRole, newTicked ->
                        role = newRole
                        ticked = newTicked
                    }
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
                    // Keep any access this editor cannot see (so it is not removed by accident)
                    val final = ticked + (member.access - allowed)
                    viewModel.updateStaffAccess(member, role, final) { err ->
                        if (err == null) onDismiss() else error = err
                    }
                }
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } }
    )
}

// ------------------------- Role drop-down + access ticks -------------------------

/**
 * Choosing a role ticks its boxes; ticking by hand switches the role to "Custom"
 * (or to the matching role when the ticks happen to equal one).
 * Only access the logged-in admin has ([allowed]) can be given to someone else.
 */
@Composable
private fun RoleAndAccessFields(
    allowed: Set<AdminPermission>,
    role: StaffRole,
    ticked: Set<AdminPermission>,
    enabled: Boolean,
    onChange: (StaffRole, Set<AdminPermission>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text("Role", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    Box {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(role.label)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StaffRole.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onChange(option, option.permissions.intersect(allowed))
                    }
                )
            }
        }
    }

    HorizontalDivider()
    Text("Access (tick what this person can see)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

    AdminPermission.entries.filter { it in allowed }.forEach { permission ->
        val checked = permission in ticked
        val toggle = {
            val newTicked = if (checked) ticked - permission else ticked + permission
            val matching = StaffRole.entries.firstOrNull {
                it != StaffRole.CUSTOM && it.permissions.intersect(allowed) == newTicked
            }
            onChange(matching ?: StaffRole.CUSTOM, newTicked)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { toggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { toggle() }, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(permission.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    permission.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}