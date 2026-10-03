package lk.nenasayura.app.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import lk.nenasayura.app.ui.common.FeaturePlaceholderScreen
import lk.nenasayura.app.ui.guest.GuestHomeScreen
import lk.nenasayura.app.ui.guest.LoginPlaceholderScreen
import lk.nenasayura.app.ui.student.StudentHomeScreen
import lk.nenasayura.app.ui.guest.papers.PapersScreen
import lk.nenasayura.app.ui.guest.markingschemes.MarkingSchemesScreen
import lk.nenasayura.app.ui.guest.guidebooks.GuideBooksScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import lk.nenasayura.app.ui.guest.quizzes.QuizListScreen
import lk.nenasayura.app.ui.guest.quizzes.QuizScreen
import lk.nenasayura.app.ui.guest.search.SearchScreen
import lk.nenasayura.app.ui.student.StudentHomeActions

/**
 * Root navigation graph of the app. The app always starts on Guest Home.
 * It follows the login state provided by the authentication layer.
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val session by currentSession()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // CHANGED: Log in click kalama oyage real Login screen (MainActivity) ekata yanawa
    val context = LocalContext.current
    val openRealLogin: () -> Unit = {
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? Activity)?.finish()
    }

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
                onLogin = openRealLogin   // CHANGED
            )
        }

        // Dan use wenne naha (onLogin dan MainActivity ekata yanawa), hebai ain karanna epa
        composable(Routes.LOGIN) {
            LoginPlaceholderScreen(onBack = { navController.popBackStack() })
        }

        // Replaced by the full Student Home in Task 7
        composable(Routes.STUDENT_HOME) {
            StudentHomeScreen(
                actions = StudentHomeActions(
                    onOpenPapers = { navController.navigate(Routes.PAPERS) },
                    onOpenMarkingSchemes = { navController.navigate(Routes.MARKING_SCHEMES) },
                    onOpenQuizzes = { navController.navigate(Routes.QUIZZES) },
                    onOpenGuideBooks = { navController.navigate(Routes.GUIDE_BOOKS) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenTeachers = { navController.navigate(Routes.TEACHERS) },
                    onOpenTimetable = { navController.navigate(Routes.TIMETABLE) },
                    onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                    onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) }
                ),
                onSignOut = { AppSession.provider.signOut() }
            )
        }

        // Placeholders: each is replaced by a real screen in its own task
        composable(Routes.PAPERS) {
            PapersScreen(onBack = { navController.popBackStack() })
        }  // Task 2
        composable(Routes.MARKING_SCHEMES) {
            MarkingSchemesScreen(onBack = { navController.popBackStack() })
        } // Task 3
        composable(Routes.GUIDE_BOOKS) {
            GuideBooksScreen(onBack = { navController.popBackStack() })
        } // Task 4
        composable(Routes.QUIZZES) {
            QuizListScreen(
                onBack = { navController.popBackStack() },
                onStartQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) }
            )
        }

        composable(
            route = Routes.QUIZ_PLAY,
            arguments = listOf(navArgument("quizId") { type = NavType.StringType })
        ) {
            QuizScreen(
                onBack = { navController.popBackStack() },
                onLogin = { navController.navigate(Routes.LOGIN) }
            )
        } // Task 5
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) }
            )
        } // Task 6
        // Student-only screens: each is replaced by a real screen in its own task
        placeholder(Routes.TEACHERS, "Teachers", navController)              // Tasks 8 and 9
        placeholder(Routes.TIMETABLE, "Timetable", navController)            // Task 10
        placeholder(Routes.REMINDERS, "Reminders", navController)            // Task 11
        placeholder(Routes.ANALYTICS, "Study Analytics", navController)      // Task 12
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