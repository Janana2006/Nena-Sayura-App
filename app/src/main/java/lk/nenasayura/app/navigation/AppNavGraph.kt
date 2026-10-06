package lk.nenasayura.app.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import lk.nenasayura.app.MainActivity
import lk.nenasayura.app.core.session.AppSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.core.session.isTeacher
import lk.nenasayura.app.ui.common.FeaturePlaceholderScreen
import lk.nenasayura.app.ui.guest.GuestHomeScreen
import lk.nenasayura.app.ui.guest.LoginPlaceholderScreen
import lk.nenasayura.app.ui.student.StudentHomeScreen
import lk.nenasayura.app.ui.teacher.TeacherHomeScreen
import lk.nenasayura.app.ui.guest.papers.PapersScreen
import lk.nenasayura.app.ui.guest.markingschemes.MarkingSchemesScreen
import lk.nenasayura.app.ui.guest.guidebooks.GuideBooksScreen

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
    //  - a student / teacher who just logged in (or an already restored session)
    //    goes to their own home
    //  - after sign-out the user goes back to Guest Home
    LaunchedEffect(session, currentRoute) {
        val homeRoute = when {
            session.isStudent -> Routes.STUDENT_HOME
            session.isTeacher -> Routes.TEACHER_HOME
            else -> null
        }
        val onRoleHome = currentRoute == Routes.STUDENT_HOME || currentRoute == Routes.TEACHER_HOME

        if (homeRoute != null && (currentRoute == Routes.GUEST_HOME || currentRoute == Routes.LOGIN)) {
            navController.navigate(homeRoute) {
                popUpTo(Routes.GUEST_HOME) { inclusive = true }
            }
        } else if (homeRoute == null && onRoleHome) {
            navController.navigate(Routes.GUEST_HOME) {
                popUpTo(currentRoute!!) { inclusive = true }
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
                onLogin = {
                    val context = navController.context
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                }
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

        // Teacher module (Step 1: shell with 4 tabs; real screens come in later steps)
        composable(Routes.TEACHER_HOME) {
            TeacherHomeScreen(onSignOut = { AppSession.provider.signOut() })
        }

        // Placeholders: each is replaced by a real screen in its own task
        composable(Routes.PAPERS) {
            PapersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MARKING_SCHEMES) {
            MarkingSchemesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GUIDE_BOOKS) {
            GuideBooksScreen(onBack = { navController.popBackStack() })
        }
        placeholder(Routes.QUIZZES, "Quizzes", navController)
        placeholder(Routes.SEARCH, "Search", navController)
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
