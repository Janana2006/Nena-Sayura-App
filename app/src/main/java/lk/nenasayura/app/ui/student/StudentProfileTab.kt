package lk.nenasayura.app.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.browse.StudentStudySync
import lk.nenasayura.app.data.model.StudentProfile
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.RoleSwitchCard
import lk.nenasayura.app.ui.student.profile.ChangeEmailScreen
import lk.nenasayura.app.ui.student.profile.ChangePasswordScreen
import lk.nenasayura.app.ui.student.profile.PersonalDetailsScreen
import lk.nenasayura.app.ui.student.profile.StudentProfileViewModel
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondary

/** The pages that open inside the Profile tab. */
private enum class ProfilePage { Personal, Email, Password, Study }

private val LockedPalette = IconPalette(Color(0xFF6B7280), Color(0xFFE5E7EB))

private const val GOOGLE_NOTE = "Your email and password are managed by your Google account"

/**
 * Profile tab of the student: header, study details (with change), personal details,
 * email, password and sign out. Each setting opens as a page inside this tab.
 */
@Composable
fun StudentProfileTab(
    name: String,
    email: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onRoleChanged: (String) -> Unit = {},
    onChangeStudy: () -> Unit = {},
    viewModel: StudentProfileViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // -1 = the profile overview, otherwise the index of the open page
    var pageIndex by rememberSaveable { mutableIntStateOf(-1) }
    val page = ProfilePage.entries.getOrNull(pageIndex)

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // Back from a page returns to the profile overview
    BackHandler(enabled = page != null) { pageIndex = -1 }

    // If the details could not be loaded, still show the name and email we already know
    val profile = state.profile ?: StudentProfile(name = name, email = email)

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.loading -> LoadingView()

            page == ProfilePage.Personal -> PersonalDetailsScreen(
                profile = profile,
                saving = state.saving,
                onBack = { pageIndex = -1 },
                onSave = { newName, phone, school, district, examYear ->
                    viewModel.saveDetails(newName, phone, school, district, examYear) {
                        pageIndex = -1
                    }
                }
            )

            page == ProfilePage.Email -> ChangeEmailScreen(
                currentEmail = profile.email,
                saving = state.saving,
                onBack = { pageIndex = -1 },
                onSubmit = { password, newEmail ->
                    viewModel.changeEmail(password, newEmail) { pageIndex = -1 }
                }
            )

            page == ProfilePage.Password -> ChangePasswordScreen(
                saving = state.saving,
                onBack = { pageIndex = -1 },
                onSubmit = { current, newPassword, confirm ->
                    viewModel.changePassword(current, newPassword, confirm) { pageIndex = -1 }
                },
                onForgot = { viewModel.sendPasswordReset(profile.email) }
            )

            else -> ProfileOverview(
                profile = profile,
                errorMessage = state.errorMessage,
                onOpen = { target ->
                    if (target == ProfilePage.Study) {
                        // Same change screens as Papers (stream, then subjects)
                        StudentStudySync.apply(profile)
                        onChangeStudy()
                    } else {
                        pageIndex = target.ordinal
                    }
                },
                onLockedClick = { viewModel.showMessage(GOOGLE_NOTE) },
                onRetry = { viewModel.load() },
                onSignOut = onSignOut,
                onRoleChanged = onRoleChanged
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileOverview(
    profile: StudentProfile,
    errorMessage: String?,
    onOpen: (ProfilePage) -> Unit,
    onLockedClick: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    onRoleChanged: (String) -> Unit
) {
    val personalSubtitle = listOf(profile.school, profile.district)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
        .ifBlank { "Name, phone, school and district" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(profile)

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Load error ----------
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }

            // ---------- Study details ----------
            StudyCard(profile = profile, onEdit = { onOpen(ProfilePage.Study) })

            // ---------- Account settings ----------
            Text(
                text = "Account settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            MenuItemCard(
                title = "Personal details",
                subtitle = personalSubtitle,
                icon = Icons.Filled.Person,
                onClick = { onOpen(ProfilePage.Personal) },
                palette = NenaIconColors.Papers
            )

            if (profile.isGoogleAccount) {
                // Google accounts: email and password are managed by Google
                MenuItemCard(
                    title = "Email address",
                    subtitle = "Managed by your Google account",
                    icon = Icons.Filled.Lock,
                    onClick = onLockedClick,
                    palette = LockedPalette
                )
                MenuItemCard(
                    title = "Password",
                    subtitle = "Managed by your Google account",
                    icon = Icons.Filled.Lock,
                    onClick = onLockedClick,
                    palette = LockedPalette
                )
            } else {
                MenuItemCard(
                    title = "Email address",
                    subtitle = profile.email.ifBlank { "Change your login email" },
                    icon = Icons.Filled.Email,
                    onClick = { onOpen(ProfilePage.Email) },
                    palette = NenaIconColors.Timetable
                )
                MenuItemCard(
                    title = "Password",
                    subtitle = "Change your password",
                    icon = Icons.Filled.Lock,
                    onClick = { onOpen(ProfilePage.Password) },
                    palette = NenaIconColors.MarkingSchemes
                )
            }

            // ---------- Teacher dashboard (switch, or become a teacher too) ----------
            RoleSwitchCard(currentRole = "student", onRoleChanged = onRoleChanged)

            Spacer(modifier = Modifier.size(4.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign out")
            }
        }
    }
}

/** Gradient header, same look as the Home header. */
@Composable
private fun ProfileHeader(profile: StudentProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(NenaPrimary, NenaSecondary)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = NenaPrimary
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = profile.name.ifBlank { "Student" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (profile.email.isNotBlank()) {
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill("Student")
                if (profile.streamName.isNotBlank()) HeaderPill(profile.streamName)
                if (profile.examYear > 0) HeaderPill("A/L ${profile.examYear}")
            }
        }
    }
}

@Composable
private fun HeaderPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun StudyCard(profile: StudentProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Study details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.size(10.dp))

            if (profile.hasStudyDetails) {
                Text(
                    text = profile.streamName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NenaPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                profile.subjectNames.forEachIndexed { index, subject ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(NenaPrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = NenaPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = subject, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                Text(
                    text = "You haven't chosen your stream and subjects yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (profile.hasStudyDetails) "Change stream & subjects" else "Choose stream & subjects")
            }
        }
    }
}