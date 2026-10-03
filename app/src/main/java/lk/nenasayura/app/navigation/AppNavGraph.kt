package lk.nenasayura.app.navigation



import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import lk.nenasayura.app.core.session.AppSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.ui.common.FeaturePlaceholderScreen
import lk.nenasayura.app.ui.guest.GuestHomeScreen
import lk.nenasayura.app.ui.guest.LoginPlaceholderScreen
import lk.nenasayura.app.ui.student.StudentHomeScreen
import lk.nenasayura.app.ui.guest.papers.PapersScreen

/**
 * Root navigation graph of the app. The app always starts on Guest Home.
 * It follows the login state provided by the authentication layer.
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val session by currentSession()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Keep the visible home in step with the login state:
    //  - a student who just logged in (or an already restored session) goes to Student Home
    //  - after sign-out the user goes back to Guest Home
    LaunchedEffect(session, currentRoute) {
        val isStudent = session.isStudent
        if (isStudent && (currentRoute == Routes.GUEST_HOME || currentRoute == Routes.LOGIN)) {
            navController.navigate(Routes.STUDENT_HOME) {
                popUpTo(Routes.GUEST_HOME) { inclusive = true }
            }
        } else if (!isStudent && currentRoute == Routes.STUDENT_HOME) {
            navController.navigate(Routes.GUEST_HOME) {
                popUpTo(Routes.STUDENT_HOME) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.GUEST_HOME) {

        composable(Routes.GUEST_HOME) {
            GuestHomeScreen(
                onOpenPapers = { navController.navigate(Routes.PAPERS) },
                onOpenMarkingSchemes = { navController.navigate(Routes.MARKING_SCHEMES) },
                onOpenQuizzes = { navController.navigate(Routes.QUIZZES) },
                onOpenGuideBooks = { navController.navigate(Routes.GUIDE_BOOKS) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        // Replaced by the authentication teammate's Login screen
        composable(Routes.LOGIN) {
            LoginPlaceholderScreen(onBack = { navController.popBackStack() })
        }

        // Replaced by the full Student Home in Task 7
        composable(Routes.STUDENT_HOME) {
            StudentHomeScreen(onSignOut = { AppSession.provider.signOut() })
        }

        // Placeholders: each is replaced by a real screen in its own task
        composable(Routes.PAPERS) {
            PapersScreen(onBack = { navController.popBackStack() })
        }                // Task 2
        placeholder(Routes.MARKING_SCHEMES, "Marking Schemes", navController) // Task 3
        placeholder(Routes.GUIDE_BOOKS, "Guide Books", navController)         // Task 4
        placeholder(Routes.QUIZZES, "Quizzes", navController)                 // Task 5
        placeholder(Routes.SEARCH, "Search", navController)                   // Task 6
    }
}

private fun NavGraphBuilder.placeholder(
    route: String,
    title: String,
    navController: NavHostController
) {
    composable(route) {
        FeaturePlaceholderScreen(title = title, onBack = { navController.popBackStack() })
    }
}