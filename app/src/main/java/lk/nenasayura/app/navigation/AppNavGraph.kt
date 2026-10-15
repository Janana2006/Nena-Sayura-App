package lk.nenasayura.app.navigation

import android.app.Activity
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import lk.nenasayura.app.MainActivity
import lk.nenasayura.app.core.browse.GuideBrowseSelection
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.core.browse.QuizBrowseSelection
import lk.nenasayura.app.core.session.AppSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.core.session.isTeacher
import lk.nenasayura.app.ui.common.FeaturePlaceholderScreen
import lk.nenasayura.app.ui.common.StudentStudyGate
import lk.nenasayura.app.ui.common.applyActiveRole
import lk.nenasayura.app.ui.guest.GuestHomeScreen
import lk.nenasayura.app.ui.guest.LoginPlaceholderScreen
import lk.nenasayura.app.ui.guest.guidebooks.GuideBooksScreen
import lk.nenasayura.app.ui.guest.guidebooks.GuideSubjectsScreen
import lk.nenasayura.app.ui.guest.markingschemes.MarkingSchemesScreen
import lk.nenasayura.app.ui.guest.papers.PapersScreen
import lk.nenasayura.app.ui.guest.quizzes.QuizListScreen
import lk.nenasayura.app.ui.guest.quizzes.QuizScreen
import lk.nenasayura.app.ui.guest.search.SearchScreen
import lk.nenasayura.app.ui.student.StudentHomeActions
import lk.nenasayura.app.ui.student.StudentHomeScreen
import lk.nenasayura.app.ui.guest.papers.PaperCategoriesScreen
import lk.nenasayura.app.ui.guest.markingschemes.MarkingSchemeCategoriesScreen
import lk.nenasayura.app.ui.guest.guidebooks.GuideBookCategoriesScreen
import lk.nenasayura.app.ui.student.QuickAccessScreen
import lk.nenasayura.app.ui.student.profile.ChangeStudyPage
import lk.nenasayura.app.ui.student.quizzes.QuizStreamSelectScreen
import lk.nenasayura.app.ui.student.quizzes.QuizSubjectSelectScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.ui.student.reviews.AllReviewsScreen
import lk.nenasayura.app.ui.student.reviews.WriteReviewScreen
import lk.nenasayura.app.ui.teacher.TeacherHomeScreen

/**
 * Root navigation graph of the app. The app always starts on Guest Home.
 * It follows the login state provided by the authentication layer.
 */
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val session by currentSession()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val context = LocalContext.current

    // Guest kenek "Log in" click kalama Login screen (MainActivity) ekata yanawa.
    // EXTRA_FROM_GUEST = true nisa Login screen eke "Continue as Guest" button eka hide wenawa.
    val openRealLogin: () -> Unit = {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_FROM_GUEST, true)
        )
        (context as? Activity)?.finish()
    }

    // Sign out kalama Login screen ekata yanawa, mehedi flag eka naha
    // nisa "Continue as Guest" button eka aye pennanawa.
    val openLoginAfterSignOut: () -> Unit = {
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? Activity)?.finish()
    }

    // Keep the visible home in step with the login state:
    //  - a student who just logged in (or an already restored session) goes to Student Home
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
        } else if (homeRoute != null && onRoleHome && currentRoute != homeRoute) {
            // The user switched role (Student <-> Teacher) from the Profile tab: open the other dashboard
            navController.navigate(homeRoute) {
                popUpTo(currentRoute!!) { inclusive = true }
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
                onLogin = openRealLogin
            )
        }

        // Dan use wenne naha (onLogin dan MainActivity ekata yanawa), hebai ain karanna epa
        composable(Routes.LOGIN) {
            LoginPlaceholderScreen(onBack = { navController.popBackStack() })
        }

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
                    onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                    onOpenQuickAccess = { navController.navigate(Routes.QUICK_ACCESS) },
                    onOpenReviews = { navController.navigate(Routes.REVIEWS) },
                    onWriteReview = { navController.navigate(Routes.WRITE_REVIEW) }
                ),
                // Teacher dashboard ekata switch (session eka wenas wenakota LaunchedEffect eka navigate karanawa)
                onRoleChanged = { role ->
                    PaperBrowseSelection.clear()
                    GuideBrowseSelection.clear()
                    applyActiveRole(role)
                },
                // Sign out kalama Firebase eken logout wela Login ekata yanawa
                onSignOut = {
                    PaperBrowseSelection.clear()
                    GuideBrowseSelection.clear()
                    FirebaseAuth.getInstance().signOut()
                    AppSession.provider.signOut()
                    openLoginAfterSignOut()
                }
            )
        }

        composable(Routes.TEACHER_HOME) {
            TeacherHomeScreen(
                // Student dashboard ekata switch
                onRoleChanged = { role -> applyActiveRole(role) },
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()
                    AppSession.provider.signOut()
                    openLoginAfterSignOut()
                }
            )
        }

        composable(Routes.PAPERS) {
            if (session.isStudent) {
                // Students: the stream + subjects chosen at sign-up are used. No extra selection step.
                StudentStudyGate(
                    title = "Papers",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                    PaperCategoriesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCategory = { category ->
                            navController.navigate(Routes.papersRoute(category))
                        },
                        selection = browse,
                        onChangeSelection = { navController.navigate(Routes.STUDY_EDIT) }
                    )
                }
            } else {
                // Guests: unchanged
                PaperCategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategory = { category ->
                        navController.navigate(Routes.papersRoute(category))
                    }
                )
            }
        }
        // "Change" button: pick a new stream + subjects (saved in the profile)
        composable(Routes.STUDY_EDIT) {
            ChangeStudyPage(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PAPERS_LIST,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
            if (session.isStudent) {
                StudentStudyGate(
                    title = "Papers",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    PapersScreen(onBack = { navController.popBackStack() })
                }
            } else {
                PapersScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(Routes.MARKING_SCHEMES) {
            if (session.isStudent) {
                // Students: the stream + subjects chosen at sign-up are used. No extra selection step.
                StudentStudyGate(
                    title = "Marking Schemes",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                    MarkingSchemeCategoriesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCategory = { category ->
                            navController.navigate(Routes.markingSchemesRoute(category))
                        },
                        selection = browse,
                        onChangeSelection = { navController.navigate(Routes.STUDY_EDIT) }
                    )
                }
            } else {
                // Guests: unchanged
                MarkingSchemeCategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategory = { category ->
                        navController.navigate(Routes.markingSchemesRoute(category))
                    }
                )
            }
        }
        composable(
            route = Routes.MARKING_SCHEMES_LIST,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
            if (session.isStudent) {
                StudentStudyGate(
                    title = "Marking Schemes",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    MarkingSchemesScreen(onBack = { navController.popBackStack() })
                }
            } else {
                MarkingSchemesScreen(onBack = { navController.popBackStack() })
            }
        }

        // ---------- Guide Books ----------
        composable(Routes.GUIDE_BOOKS) {
            if (session.isStudent) {
                // Students: the stream + subjects chosen at sign-up are used. No extra selection step.
                StudentStudyGate(
                    title = "Guide Books",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    val guideBrowse by GuideBrowseSelection.state.collectAsStateWithLifecycle()
                    GuideBookCategoriesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCategory = { category ->
                            navController.navigate(Routes.guideBooksRoute(category))
                        },
                        selection = guideBrowse,
                        onChangeSelection = { navController.navigate(Routes.STUDY_EDIT) }
                    )
                }
            } else {
                // Guests: unchanged
                GuideBookCategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategory = { category ->
                        navController.navigate(Routes.guideBooksRoute(category))
                    }
                )
            }
        }
        composable(
            route = Routes.GUIDE_BOOKS_LIST,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("subjectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val category = entry.arguments?.getString("category") ?: "guide"
            val subjectId = entry.arguments?.getString("subjectId")
            if (session.isStudent) {
                StudentStudyGate(
                    title = "Guide Books",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    if (subjectId == null) {
                        // Only the student's own subjects
                        GuideSubjectsScreen(
                            category = category,
                            onBack = { navController.popBackStack() },
                            onOpenSubject = { selectedSubId ->
                                navController.navigate(Routes.guideBooksRoute(category, selectedSubId))
                            }
                        )
                    } else {
                        GuideBooksScreen(onBack = { navController.popBackStack() })
                    }
                }
            } else {
                // Guests
                GuideBooksScreen(onBack = { navController.popBackStack() })
            }
        }

        composable(Routes.QUIZZES) {
            if (session.isStudent) {
                // Students: choose a stream first (this session only), then choose a subject
                val quizBrowse by QuizBrowseSelection.state.collectAsStateWithLifecycle()
                if (quizBrowse.hasSelection) {
                    QuizSubjectSelectScreen(
                        onBack = { navController.popBackStack() },
                        onChangeStream = { QuizBrowseSelection.clear() },
                        onSubjectSelected = { subjectId ->
                            navController.navigate(Routes.quizzesBySubjectRoute(subjectId))
                        }
                    )
                } else {
                    QuizStreamSelectScreen(
                        onBack = { navController.popBackStack() },
                        onStreamSelected = { stream -> QuizBrowseSelection.select(stream) }
                    )
                }
            } else {
                // Guests: open all quizzes with subject filter chip
                QuizListScreen(
                    onBack = { navController.popBackStack() },
                    onStartQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) }
                )
            }
        }
        composable(
            route = Routes.QUIZZES_BY_SUBJECT,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) {
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
                onLogin = openRealLogin
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) }
            )
        }
        composable(Routes.QUICK_ACCESS) {
            QuickAccessScreen(
                onBack = { navController.popBackStack() },
                onOpenPapers = { navController.navigate(Routes.PAPERS) },
                onOpenQuizzes = { navController.navigate(Routes.QUIZZES) },
                onOpenTimetable = { navController.navigate(Routes.TIMETABLE) },
                onOpenReminders = { navController.navigate(Routes.REMINDERS) },
                onOpenMarkingSchemes = { navController.navigate(Routes.MARKING_SCHEMES) },
                onOpenGuideBooks = { navController.navigate(Routes.GUIDE_BOOKS) }
            )
        }
        composable(Routes.REVIEWS) {
            AllReviewsScreen(
                onBack = { navController.popBackStack() },
                onWriteReview = { navController.navigate(Routes.WRITE_REVIEW) }
            )
        }
        composable(Routes.WRITE_REVIEW) {
            WriteReviewScreen(onBack = { navController.popBackStack() })
        }
        // Student-only screens: each is replaced by a real screen in its own task
        placeholder(Routes.TEACHERS, "Teachers", navController)
        placeholder(Routes.TIMETABLE, "Timetable", navController)
        placeholder(Routes.REMINDERS, "Reminders", navController)
        placeholder(Routes.ANALYTICS, "Study Analytics", navController)
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