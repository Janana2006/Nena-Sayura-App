package lk.nenasayura.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import lk.nenasayura.app.ui.common.EmptyView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer

private enum class AdminTab(val label: String, val title: String, val icon: ImageVector) {
    Home("Home", "Home", Icons.Filled.Home),
    Users("Users", "Students & Teachers", Icons.Filled.People),
    Payments("Payments", "Payments", Icons.Filled.Payments),
    Content("Content", "Manage Content", Icons.Filled.Folder),
    Staff("Staff", "Staff & Account", Icons.Filled.AdminPanelSettings)
}

/**
 * Home of a logged-in admin, with a bottom navigation bar.
 * Same layout as the Teacher and Student homes, so every role feels like one app.
 */
@Composable
fun AdminHomeScreen(
    onSignOut: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val firebaseUser = remember { FirebaseAuth.getInstance().currentUser }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(firebaseUser?.uid) {
        if (firebaseUser != null) viewModel.start(firebaseUser.uid) else onSignOut()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = AdminTab.entries[selectedIndex]
    BackHandler(enabled = selectedTab != AdminTab.Home) { selectedIndex = 0 }

    val ready = !state.checking && !state.accessDenied && !state.loading

    Scaffold(
        topBar = {
            // The Home tab draws its own gradient header
            if (ready && selectedTab != AdminTab.Home) {
                NenaTopBar(
                    title = selectedTab.title,
                    actions = {
                        IconButton(onClick = viewModel::reload) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (ready) {
                NavigationBar {
                    AdminTab.entries.forEachIndexed { index, tab ->
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.checking -> LoadingView(modifier = Modifier.padding(innerPadding))

                state.accessDenied -> EmptyView(
                    message = "This account does not have admin access.",
                    modifier = Modifier.padding(innerPadding),
                    actionLabel = "Sign out",
                    onAction = onSignOut
                )

                state.loading -> LoadingView(modifier = Modifier.padding(innerPadding))

                else -> when (selectedTab) {
                    // Home goes edge to edge at the top, so only the bottom padding is applied
                    AdminTab.Home -> AdminDashboardTab(
                        state = state,
                        onOpenUsers = { selectedIndex = AdminTab.Users.ordinal },
                        onOpenPayments = { selectedIndex = AdminTab.Payments.ordinal },
                        onOpenContent = { selectedIndex = AdminTab.Content.ordinal },
                        onOpenStaff = { selectedIndex = AdminTab.Staff.ordinal },
                        onRefresh = viewModel::reload,
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    )

                    AdminTab.Users -> AdminUsersTab(
                        users = state.users,
                        payments = state.payments,
                        modifier = Modifier.padding(innerPadding)
                    )

                    AdminTab.Payments -> AdminPaymentsTab(
                        state = state,
                        onSaveCommission = viewModel::setCommission,
                        modifier = Modifier.padding(innerPadding)
                    )

                    AdminTab.Content -> AdminContentTab(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )

                    AdminTab.Staff -> AdminStaffTab(
                        state = state,
                        viewModel = viewModel,
                        onSignOut = onSignOut,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            if (state.busy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}