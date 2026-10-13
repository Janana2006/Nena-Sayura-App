package lk.nenasayura.app.ui.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.data.model.AdminPermission
import lk.nenasayura.app.ui.theme.NenaAccent
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer
import lk.nenasayura.app.util.Validators

/**
 * Admin Profile Tab.
 *
 * Top: a gradient "ID badge" header (initials avatar, role, member since) with a floating stats strip.
 * Then: tap-to-edit display name, what this admin can access, account actions
 * (reset password / sign out, both with a confirm step) and the staff team (if permitted).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminProfileTab(
    state: AdminUiState,
    viewModel: AdminViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    profileVm: AdminProfileViewModel = viewModel()
) {
    val profileState by profileVm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingName by rememberSaveable { mutableStateOf(false) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    var changingEmail by remember { mutableStateOf(false) }
    var emailSentTo by remember { mutableStateOf<String?>(null) }

    // Back from the email app after opening the verification link: pick up the new email
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { profileVm.refreshAccount() }

    val displayName = profileState.name.ifBlank { "Admin" }
    val myRole = state.staff
        .firstOrNull { it.email.equals(profileState.email, ignoreCase = true) }
        ?.staffRoleLabel ?: "Full Admin"

    // Three numbers for the floating strip, only what this admin is allowed to see
    val stats = buildList {
        if (state.can(AdminPermission.USERS)) {
            add("Students" to state.students.size.toString())
            add("Teachers" to state.teachers.size.toString())
        }
        if (state.can(AdminPermission.STAFF)) add("Staff" to state.staff.size.toString())
        if (size < 3) add("Access" to "${state.access.size}/${AdminPermission.entries.size}")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NenaBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ---------------- Header + floating stats ----------------
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)))
                    .drawBehind {
                        // Soft rings in the corners, a little depth without any images
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = size.width * 0.38f,
                            center = Offset(size.width * 0.96f, size.height * 0.08f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = size.width * 0.26f,
                            center = Offset(size.width * 0.02f, size.height * 0.92f)
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar with a ring and a "verified" badge
                    Box(modifier = Modifier.size(96.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.22f), CircleShape)
                                .padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initialsOf(profileState.name, profileState.email),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NenaPrimary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .background(NenaAccent, CircleShape)
                                .padding(5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = profileState.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeaderChip(text = myRole, icon = Icons.Filled.AdminPanelSettings)
                        if (profileState.memberSince > 0L) {
                            HeaderChip(
                                text = "Joined ${formatDate(profileState.memberSince)}",
                                icon = Icons.Filled.CalendarMonth
                            )
                        }
                    }
                }
            }

            // Floating strip: sits half on the header, half on the page
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 36.dp)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    stats.forEachIndexed { index, (label, value) ->
                        if (index > 0) VerticalDivider(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NenaPrimary
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Room for the half of the strip that hangs below the header
        Spacer(modifier = Modifier.height(36.dp))

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------------- Display name (tap the pencil to edit) ----------------
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Filled.Badge, NenaPrimary, NenaPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display name",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!editingName) {
                        IconButton(onClick = {
                            draftName = profileState.name
                            editingName = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit name", tint = NenaPrimary)
                        }
                    }
                }

                AnimatedVisibility(visible = editingName) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = { draftName = it },
                            label = { Text("New display name") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { editingName = false },
                                enabled = !profileState.busy,
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    profileVm.updateName(draftName) { err ->
                                        if (err == null) editingName = false
                                        Toast.makeText(
                                            context,
                                            err ?: "Name updated",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = !profileState.busy &&
                                        draftName.isNotBlank() &&
                                        draftName.trim() != profileState.name,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (profileState.busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text("Save name")
                                }
                            }
                        }
                    }
                }
            }

            // ---------------- What this admin can open ----------------
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(Icons.Filled.Key, NenaSecondary, NenaSecondaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Your access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${state.access.size} of ${AdminPermission.entries.size} areas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (state.access.isEmpty()) {
                    Text(
                        text = "Home only. Ask the main admin if you need more.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminPermission.entries.filter { it in state.access }.forEach { permission ->
                            AccessChip(permission.label)
                        }
                    }
                }
            }

            // ---------------- Account actions ----------------
            AdminCard {
                ActionRow(
                    icon = Icons.Filled.Email,
                    title = "Change email",
                    subtitle = if (profileState.hasPasswordLogin) {
                        "Now: ${profileState.email.ifBlank { "not set" }}"
                    } else {
                        "You sign in with Google. Change it in your Google account."
                    },
                    tint = NenaSecondary,
                    container = NenaSecondaryContainer,
                    enabled = profileState.hasPasswordLogin && !profileState.busy,
                    onClick = {
                        changingEmail = !changingEmail
                        changingPassword = false
                    }
                )
                AnimatedVisibility(visible = changingEmail && profileState.hasPasswordLogin) {
                    ChangeEmailForm(
                        currentEmail = profileState.email,
                        busy = profileState.busy,
                        onSubmit = { newEmail, password, done ->
                            profileVm.changeEmail(newEmail, password) { err ->
                                if (err == null) emailSentTo = newEmail.trim()
                                else Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                done(err == null)
                            }
                        },
                        onClose = { changingEmail = false }
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = Color(0xFFF1F5F9)
                )
                ActionRow(
                    icon = Icons.Filled.Lock,
                    title = "Change password",
                    subtitle = if (profileState.hasPasswordLogin) {
                        "Set a new one right here, no email needed"
                    } else {
                        "You sign in with Google. Change it in your Google account."
                    },
                    tint = NenaPrimary,
                    container = NenaPrimaryContainer,
                    enabled = profileState.hasPasswordLogin && !profileState.busy,
                    onClick = {
                        changingPassword = !changingPassword
                        changingEmail = false
                    }
                )
                AnimatedVisibility(visible = changingPassword && profileState.hasPasswordLogin) {
                    ChangePasswordForm(
                        busy = profileState.busy,
                        onSubmit = { current, new, done ->
                            profileVm.changePassword(current, new) { err ->
                                Toast.makeText(context, err ?: "Password changed", Toast.LENGTH_LONG).show()
                                done(err == null)
                            }
                        },
                        onClose = { changingPassword = false },
                        onForgot = { confirmReset = true }
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = Color(0xFFF1F5F9)
                )
                ActionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Sign out",
                    subtitle = "Leave the admin app on this phone",
                    tint = MaterialTheme.colorScheme.error,
                    container = MaterialTheme.colorScheme.errorContainer,
                    enabled = true,
                    onClick = { confirmSignOut = true }
                )
            }

            // ---------------- Team (only with "Manage Staff") ----------------
            if (state.can(AdminPermission.STAFF)) {
                Spacer(modifier = Modifier.height(6.dp))
                AdminStaffTab(state = state, viewModel = viewModel)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Send reset email?") },
            text = { Text("A link to choose a new password will be sent to ${profileState.email}.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    profileVm.sendPasswordReset { err ->
                        Toast.makeText(
                            context,
                            err ?: "Reset email sent to ${profileState.email}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }) { Text("Send email") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }

    emailSentTo?.let { sent ->
        AlertDialog(
            onDismissRequest = { emailSentTo = null },
            title = { Text("Check your new inbox") },
            text = {
                Text(
                    "We sent a link to $sent. Open it to finish changing your email, then come back to the app. " +
                            "Until then you keep logging in with ${profileState.email}."
                )
            },
            confirmButton = { TextButton(onClick = { emailSentTo = null }) { Text("Got it") } }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to log in again to open the admin app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Stay signed in") } }
        )
    }
}

// ------------------------------- Shared helpers -------------------------------

/** "JJ" for "janana Jayasuiya", first letter for one word, or from the email when there is no name. */
internal fun initialsOf(name: String, email: String): String {
    val source = name.trim().ifBlank { email.substringBefore('@') }
    val parts = source.split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "A"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Round avatar with initials, used for the staff list. */
@Composable
internal fun InitialsAvatar(
    initials: String,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun HeaderChip(text: String, icon: ImageVector) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color, container: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(container, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AccessChip(label: String) {
    Surface(shape = RoundedCornerShape(50), color = NenaSecondaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = NenaSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = NenaSecondary
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    container: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon, tint, container)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (title == "Sign out") tint else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Current password + new password + confirm. Clears itself after a successful change. */
@Composable
private fun ChangePasswordForm(
    busy: Boolean,
    onSubmit: (current: String, new: String, done: (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit,
    onForgot: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val mismatch = confirm.isNotEmpty() && confirm != newPassword
    val canSave = !busy && current.isNotEmpty() &&
            Validators.isValidPassword(newPassword) && newPassword == confirm

    fun reset() {
        current = ""
        newPassword = ""
        confirm = ""
        visible = false
    }

    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PasswordField("Current password", current, { current = it }, visible, { visible = !visible })
        PasswordField(
            label = "New password",
            value = newPassword,
            onChange = { newPassword = it },
            visible = visible,
            onToggle = { visible = !visible },
            supporting = "At least 6 characters"
        )
        PasswordField(
            label = "Confirm new password",
            value = confirm,
            onChange = { confirm = it },
            visible = visible,
            onToggle = { visible = !visible },
            isError = mismatch,
            supporting = if (mismatch) "Passwords do not match" else null
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    reset()
                    onClose()
                },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = {
                    onSubmit(current, newPassword) { ok ->
                        if (ok) {
                            reset()
                            onClose()
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.weight(1f)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Save password")
                }
            }
        }
        TextButton(onClick = onForgot, enabled = !busy) {
            Text("Forgot current password? Email me a reset link")
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit,
    isError: Boolean = false,
    supporting: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = if (supporting != null) {
            { Text(supporting) }
        } else {
            null
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/** New email + current password. The email only changes after the link sent to it is opened. */
@Composable
private fun ChangeEmailForm(
    currentEmail: String,
    busy: Boolean,
    onSubmit: (newEmail: String, password: String, done: (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    val canSend = !busy && Validators.isValidEmail(newEmail) &&
            !newEmail.trim().equals(currentEmail, ignoreCase = true) && password.isNotEmpty()

    fun reset() {
        newEmail = ""
        password = ""
        visible = false
    }

    Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            label = { Text("New email") },
            singleLine = true,
            supportingText = { Text("We send a link to this address. Your email changes after you open it.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        PasswordField(
            label = "Current password",
            value = password,
            onChange = { password = it },
            visible = visible,
            onToggle = { visible = !visible },
            supporting = "To confirm it is you"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    reset()
                    onClose()
                },
                enabled = !busy,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = {
                    onSubmit(newEmail, password) { ok ->
                        if (ok) {
                            reset()
                            onClose()
                        }
                    }
                },
                enabled = canSend,
                modifier = Modifier.weight(1f)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Send link")
                }
            }
        }
    }
}