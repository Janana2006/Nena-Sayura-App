package lk.nenasayura.app.ui.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.teacher.content.MyContentTab
import lk.nenasayura.app.ui.teacher.dashboard.TeacherDashboardScreen
import lk.nenasayura.app.ui.teacher.profile.TeacherProfileTab
import lk.nenasayura.app.ui.teacher.upload.UploadScreen
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer

private enum class TeacherTab(val label: String, val title: String, val icon: ImageVector) {
    Home("Home", "Home", Icons.Filled.Home),
    Content("My Content", "My Content", Icons.Filled.Folder),
    Upload("Upload", "Upload Material", Icons.Filled.CloudUpload),
    Profile("Profile", "My Profile", Icons.Filled.Person)
}

/**
 * Home of a logged-in teacher, with a bottom navigation bar.
 * Same layout as the Student Home, so both roles feel like one app.
 */
@Composable
fun TeacherHomeScreen(
    onSignOut: () -> Unit,
    onRoleChanged: (String) -> Unit = {},
    viewModel: TeacherViewModel = viewModel()
) {
    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(firebaseUser?.uid) {
        if (firebaseUser != null) {
            viewModel.start(firebaseUser.uid, firebaseUser.displayName ?: "", firebaseUser.email ?: "")
        } else {
            onSignOut()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // Saved as a number so the selected tab survives rotation
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = TeacherTab.entries[selectedIndex]

    // Back from another tab returns to the Home tab first
    BackHandler(enabled = selectedTab != TeacherTab.Home) { selectedIndex = 0 }

    Scaffold(
        topBar = {
            // The Home tab draws its own gradient header
            if (selectedTab != TeacherTab.Home) {
                NenaTopBar(title = selectedTab.title)
            }
        },
        bottomBar = {
            NavigationBar {
                TeacherTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NenaPrimary,
                            selectedTextColor = NenaPrimary,
                            indicatorColor = NenaPrimaryContainer
                        )
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val profile = state.profile

        when {
            state.loading -> LoadingView(modifier = Modifier.padding(innerPadding))

            profile == null -> ErrorView(
                message = state.errorMessage ?: "Could not load your profile.",
                onRetry = viewModel::reload,
                modifier = Modifier.padding(innerPadding)
            )

            else -> when (selectedTab) {
                // Home goes edge to edge at the top, so only the bottom padding is applied
                TeacherTab.Home -> TeacherDashboardScreen(
                    profile = profile,
                    contents = state.contents,
                    onOpenUpload = { selectedIndex = TeacherTab.Upload.ordinal },
                    onOpenContent = { selectedIndex = TeacherTab.Content.ordinal },
                    onOpenProfile = { selectedIndex = TeacherTab.Profile.ordinal },
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )

                TeacherTab.Content -> MyContentTab(
                    contents = state.contents,
                    refreshing = state.contentRefreshing,
                    isFreeTeacher = profile.isFree,
                    onUploadClick = { selectedIndex = TeacherTab.Upload.ordinal },
                    onRefresh = { viewModel.refreshContent() },
                    onDelete = viewModel::deleteContent,
                    onEdit = viewModel::updateContent,
                    modifier = Modifier.padding(innerPadding)
                )

                TeacherTab.Upload -> UploadScreen(
                    profile = profile,
                    onUploaded = {
                        viewModel.refreshContent(message = "Uploaded. Waiting for admin approval.")
                        selectedIndex = TeacherTab.Content.ordinal
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                TeacherTab.Profile -> TeacherProfileTab(
                    profile = profile,
                    streamSubjects = state.streamSubjects,
                    saving = state.saving,
                    onLoadSubjects = viewModel::loadStreamSubjects,
                    onSave = viewModel::saveProfile,
                    onSignOut = onSignOut,
                    onRoleChanged = onRoleChanged,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}