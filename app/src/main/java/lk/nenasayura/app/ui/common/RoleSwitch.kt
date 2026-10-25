package lk.nenasayura.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import lk.nenasayura.app.ui.theme.NenaBackground
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.nenasayura.app.core.session.FirebaseSessionProvider
import lk.nenasayura.app.data.repository.RoleRepository
import lk.nenasayura.app.screens.SetupScreen
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.viewmodel.SetupViewModel

/**
 * Makes [role] ("student" or "teacher") the role of the current in-app session.
 * The host screen then shows that role's dashboard.
 */
fun applyActiveRole(role: String) {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val name = user.displayName ?: ""
    val email = user.email ?: ""
    if (role == "teacher") {
        FirebaseSessionProvider.signInTeacher(user.uid, name, email)
    } else {
        FirebaseSessionProvider.signInStudent(user.uid, name, email)
    }
}

data class RoleSwitchUiState(
    val loading: Boolean = true,
    val info: RoleRepository.RoleInfo? = null,
    val switching: Boolean = false,
    val error: String? = null
)

class RoleSwitchViewModel(
    private val repo: RoleRepository = RoleRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(RoleSwitchUiState())
    val state: StateFlow<RoleSwitchUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                val info = repo.getInfo()
                _state.update { it.copy(loading = false, info = info, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not check your roles") }
            }
        }
    }

    fun switchTo(role: String, onDone: () -> Unit) {
        _state.update { it.copy(switching = true, error = null) }
        viewModelScope.launch {
            try {
                repo.switchTo(role)
                _state.update { it.copy(switching = false) }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(switching = false, error = e.localizedMessage ?: "Could not switch") }
            }
        }
    }
}

/**
 * Card shown in the Profile tab of BOTH dashboards.
 *  - account already has the other role  -> "Switch to ... dashboard"
 *  - account does not have it yet        -> "Become a teacher / student too" (short setup, no new registration)
 *
 * [currentRole] is the dashboard the user is on now. [onRoleChanged] is called with the new role
 * after the switch (or after the new role was added) so the host can open the other dashboard.
 */
@Composable
fun RoleSwitchCard(
    currentRole: String,
    onRoleChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoleSwitchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    val other = if (currentRole == "teacher") "student" else "teacher"
    val otherLabel = if (other == "teacher") "Teacher" else "Student"
    val info = state.info
    val hasOther = info != null && other in info.roles

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Switch role",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    hasOther -> "You also have a $otherLabel dashboard on this account."
                    other == "teacher" -> "Share notes and resources with students using the same account."
                    else -> "Practice papers and quizzes using the same account."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)

                info == null -> TextButton(onClick = { viewModel.load() }) {
                    Text(state.error ?: "Retry")
                }

                hasOther -> Button(
                    onClick = {
                        viewModel.switchTo(other) { onRoleChanged(other) }
                    },
                    enabled = !state.switching,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.switching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Switch to $otherLabel dashboard")
                    }
                }

                else -> OutlinedButton(
                    onClick = { adding = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (other == "teacher") "Become a teacher too" else "Use as a student too")
                }
            }

            if (state.error != null && info != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (adding) {
        Dialog(
            onDismissRequest = { adding = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BecomeRoleFlow(
                    role = other,
                    onCancel = { adding = false },
                    onDone = {
                        adding = false
                        onRoleChanged(other)
                    }
                )
            }
        }
    }
}

/**
 * Short setup for a role the account does not have yet:
 * teacher = Free/Paid (+ fee) then stream + 1 subject, student = stream + 3 subjects.
 * Nothing about the existing role is changed.
 */
@Composable
private fun BecomeRoleFlow(
    role: String,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    var serviceChosen by remember { mutableStateOf(role != "teacher") }
    var isFree by remember { mutableStateOf(true) }
    var feeText by remember { mutableStateOf("") }
    var feeError by remember { mutableStateOf<String?>(null) }

    if (!serviceChosen) {
        Column(modifier = Modifier.fillMaxSize().background(NenaBackground)) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ================= Header (SetupScreen eke wage) =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 60.dp, y = (-50).dp)
                            .size(180.dp)
                            .background(Color.White.copy(alpha = 0.07f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-50).dp, y = 40.dp)
                            .size(140.dp)
                            .background(Color.White.copy(alpha = 0.06f), CircleShape)
                    )

                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp).background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = NenaPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Become a teacher",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Choose how your teaching service works",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Your student account stays as it is.",
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                                .padding(14.dp)
                        )
                    }
                }

                // ================= Content =================
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        "Your service",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoleInkText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Tap how you want to offer your teaching",
                        fontSize = 13.sp,
                        color = RoleGreyText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ServiceOptionCard(
                        title = "Free",
                        description = "Share notes and resources with students at no cost",
                        icon = Icons.Default.VolunteerActivism,
                        selected = isFree,
                        onClick = { isFree = true; feeError = null }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Paid is locked for now (coming soon)
                    ServiceOptionCard(
                        title = "Paid",
                        description = "Coming soon",
                        icon = Icons.Default.Payments,
                        selected = false,
                        locked = true,
                        onClick = { }
                    )

                    if (!isFree) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Fee amount",
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoleInkText
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = feeText,
                                    onValueChange = {
                                        feeText = it.filter { c -> c.isDigit() }
                                        feeError = null
                                    },
                                    label = { Text("Fee amount") },
                                    prefix = { Text("Rs. ") },
                                    singleLine = true,
                                    isError = feeError != null,
                                    supportingText = feeError?.let { { Text(it) } },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NenaPrimary,
                                        focusedLabelColor = NenaPrimary,
                                        cursorColor = NenaPrimary
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ================= Bottom bar (SetupScreen eke wage) =================
            Surface(color = Color.White, shadowElevation = 16.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (!isFree && (feeText.toIntOrNull() ?: 0) <= 0) {
                                feeError = "Enter a valid fee amount"
                            } else {
                                serviceChosen = true
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NenaPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "Cancel",
                            color = RoleGreyText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    } else {
        val setupViewModel: SetupViewModel = viewModel()
        SetupScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = setupViewModel,
            role = role,
            onDone = onDone,
            onLogout = onCancel,
            addRole = true,
            isFree = isFree,
            fee = if (isFree) 0 else (feeText.toIntOrNull() ?: 0),
            bottomButtonLabel = "Cancel",
            blockBack = false
        )
    }
}

private val RoleInkText = Color(0xFF1B1B1F)
private val RoleGreyText = Color(0xFF6B7280)

/** Selectable card (Free / Paid) - same look as the stream cards in SetupScreen. */
@Composable
private fun ServiceOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    locked: Boolean = false
) {
    val borderColor by animateColorAsState(
        if (selected) NenaPrimary else Color(0xFFE3E6EF), label = "serviceBorder"
    )
    val bg by animateColorAsState(
        if (selected) NenaPrimaryContainer else Color.White, label = "serviceBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (locked) 0.55f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(enabled = !locked, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (selected) Brush.linearGradient(listOf(NenaPrimary, NenaSecondary))
                    else Brush.linearGradient(listOf(NenaPrimaryContainer, NenaPrimaryContainer)),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else NenaPrimary
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) NenaPrimary else RoleInkText
            )
            Text(text = description, fontSize = 12.sp, color = RoleGreyText)
        }
        if (locked) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = RoleGreyText)
        } else if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NenaSecondary)
        }
    }
}