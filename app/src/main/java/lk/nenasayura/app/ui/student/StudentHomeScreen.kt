package lk.nenasayura.app.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import lk.nenasayura.app.core.session.UserSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer

/** Every screen the Student Home can open. AppNavGraph decides where each one goes. */
data class StudentHomeActions(
    val onOpenPapers: () -> Unit,
    val onOpenMarkingSchemes: () -> Unit,
    val onOpenQuizzes: () -> Unit,
    val onOpenGuideBooks: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenTeachers: () -> Unit,
    val onOpenTimetable: () -> Unit,
    val onOpenReminders: () -> Unit,
    val onOpenAnalytics: () -> Unit
)

private enum class StudentTab(val label: String, val title: String, val icon: ImageVector) {
    Home("Home", "Home", Icons.Filled.Home),
    Resources("Resources", "Resources", Icons.AutoMirrored.Filled.LibraryBooks),
    Study("Study", "Study Tools", Icons.Filled.CalendarMonth),
    Profile("Profile", "Profile", Icons.Filled.Person)
}

/**
 * Home of a logged-in student, with a bottom navigation bar.
 * The four tabs are shown here; every other screen opens through [actions].
 */
@Composable
fun StudentHomeScreen(
    actions: StudentHomeActions,
    onSignOut: () -> Unit
) {
    val session by currentSession()
    val loggedIn = session as? UserSession.LoggedIn
    val name = loggedIn?.name?.ifBlank { null } ?: "Student"
    val email = loggedIn?.email ?: ""

    // Saved as a number so the selected tab survives opening another screen and coming back
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = StudentTab.entries[selectedIndex]

    // Back from another tab returns to the Home tab first
    BackHandler(enabled = selectedTab != StudentTab.Home) { selectedIndex = 0 }

    Scaffold(
        topBar = {
            // The Home tab draws its own gradient header
            if (selectedTab != StudentTab.Home) {
                NenaTopBar(title = selectedTab.title)
            }
        },
        bottomBar = {
            NavigationBar {
                StudentTab.entries.forEachIndexed { index, tab ->
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (selectedTab) {
            // Home goes edge to edge at the top, so only the bottom padding is applied
            StudentTab.Home -> StudentHomeTab(
                name = name,
                actions = actions,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            )

            StudentTab.Resources -> StudentResourcesTab(
                actions = actions,
                modifier = Modifier.padding(innerPadding)
            )

            StudentTab.Study -> StudentStudyTab(
                actions = actions,
                modifier = Modifier.padding(innerPadding)
            )

            StudentTab.Profile -> StudentProfileTab(
                name = name,
                email = email,
                onSignOut = onSignOut,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}