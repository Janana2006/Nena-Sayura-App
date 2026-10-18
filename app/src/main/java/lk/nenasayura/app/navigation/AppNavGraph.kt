package lk.nenasayura.app.navigation

import android.app.Activity
import android.content.Intent
import android.widget.Toast
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
import lk.nenasayura.app.core.browse.GuideBrowseState
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.core.browse.StudentStudySync
import lk.nenasayura.app.core.session.AppSession
import lk.nenasayura.app.core.session.currentSession
import lk.nenasayura.app.core.session.isStudent
import lk.nenasayura.app.core.session.isTeacher
import lk.nenasayura.app.data.model.StudyStream
import lk.nenasayura.app.data.repository.SetupRepository
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
import lk.nenasayura.app.core.reminders.ReminderScheduler
import lk.nenasayura.app.ui.student.reminders.RemindersScreen
import lk.nenasayura.app.ui.student.downloads.DownloadsScreen
import lk.nenasayura.app.ui.student.papers.StreamSelectScreen
import lk.nenasayura.app.ui.student.papers.SubjectSelectScreen
import lk.nenasayura.app.ui.student.quizzes.QuizSubjectSelectScreen
import lk.nenasayura.app.ui.student.quizzes.QuizListScreen as StudentQuizListScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lk.nenasayura.app.ui.student.reviews.AllReviewsScreen
import lk.nenasayura.app.ui.student.reviews.WriteReviewScreen
import lk.nenasayura.app.ui.student.teachercontent.TeacherListScreen
import lk.nenasayura.app.ui.student.teachercontent.TeacherMaterialsScreen
import lk.nenasayura.app.ui.student.teachercontent.TeacherPaymentScreen
import lk.nenasayura.app.ui.teacher.TeacherHomeScreen
import lk.nenasayura.app.ui.student.progress.StudyProgressScreen
import lk.nenasayura.app.ui.student.timetable.PlannerSetupScreen
import lk.nenasayura.app.ui.student.timetable.TimetableScreen

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
        // Stop every study reminder of the student who is signing out
        ReminderScheduler.cancelAndClearAsync(context)
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
                    onWriteReview = { navController.navigate(Routes.WRITE_REVIEW) },
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onChangeStudy = { navController.navigate(Routes.STUDY_EDIT) }
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
                // Guests: choose stream + subjects first (this session only), then the categories
                val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                if (browse.hasSelection) {
                    PaperCategoriesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCategory = { category ->
                            navController.navigate(Routes.papersRoute(category))
                        },
                        selection = browse,
                        onChangeSelection = {
                            navController.navigate(Routes.guestStreamsRoute(Routes.PAPERS))
                        }
                    )
                } else {
                    StreamSelectScreen(
                        onBack = { navController.popBackStack() },
                        onStreamSelected = { streamId ->
                            navController.navigate(Routes.guestSubjectsRoute(Routes.PAPERS, streamId))
                        },
                        title = "Papers"
                    )
                }
            }
        }
        // "Change" button: pick a new stream + subjects (saved in the profile).
        // Uses the same two screens as the guest flow (stream, then subjects).
        composable(Routes.STUDY_EDIT) {
            StreamSelectScreen(
                onBack = { navController.popBackStack() },
                onStreamSelected = { streamId ->
                    navController.navigate(Routes.studyEditSubjectsRoute(streamId))
                },
                title = "Change subjects"
            )
        }
        composable(
            route = Routes.STUDY_EDIT_SUBJECTS,
            arguments = listOf(navArgument("streamId") { type = NavType.StringType })
        ) {
            val editContext = LocalContext.current
            SubjectSelectScreen(
                onBack = { navController.popBackStack() },
                // SubjectSelectScreen already put the new choice into PaperBrowseSelection.
                // Here it is also saved in the profile, so it stays after the app is closed.
                onDone = {
                    val choice = PaperBrowseSelection.state.value
                    val stream = StudyStream(
                        id = choice.streamId,
                        name = choice.streamName,
                        subjects = choice.subjects
                    )
                    SetupRepository().saveSetup("student", stream, choice.subjects) { result ->
                        result.fold(
                            onSuccess = {
                                // Drop the old choice so every screen reloads the new one from the profile
                                StudentStudySync.onStudyChanged()
                                navController.popBackStack(Routes.STUDY_EDIT, true)
                            },
                            onFailure = { e ->
                                Toast.makeText(
                                    editContext,
                                    e.localizedMessage ?: "Could not save your subjects",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                },
                title = "Change subjects",
                hint = "These subjects will be saved in your profile"
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
                // Guests: choose stream + subjects first (this session only), then the categories
                val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                if (browse.hasSelection) {
                    MarkingSchemeCategoriesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCategory = { category ->
                            navController.navigate(Routes.markingSchemesRoute(category))
                        },
                        selection = browse,
                        onChangeSelection = {
                            navController.navigate(Routes.guestStreamsRoute(Routes.MARKING_SCHEMES))
                        }
                    )
                } else {
                    StreamSelectScreen(
                        onBack = { navController.popBackStack() },
                        onStreamSelected = { streamId ->
                            navController.navigate(Routes.guestSubjectsRoute(Routes.MARKING_SCHEMES, streamId))
                        },
                        title = "Marking Schemes"
                    )
                }
            }
        }

        // ---------- Guests: choose stream + subjects (also used by the "Change" button) ----------
        composable(
            route = Routes.GUEST_STREAMS,
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { entry ->
            val target = entry.arguments?.getString("target") ?: Routes.PAPERS
            StreamSelectScreen(
                onBack = { navController.popBackStack() },
                onStreamSelected = { streamId ->
                    navController.navigate(Routes.guestSubjectsRoute(target, streamId))
                },
                title = browseTitle(target)
            )
        }
        composable(
            route = Routes.GUEST_SUBJECTS,
            arguments = listOf(
                navArgument("target") { type = NavType.StringType },
                navArgument("streamId") { type = NavType.StringType }
            )
        ) { entry ->
            val target = entry.arguments?.getString("target") ?: Routes.PAPERS
            SubjectSelectScreen(
                onBack = { navController.popBackStack() },
                // The choice is saved: go back to the Papers / Marking Schemes / Guide Books / Quizzes landing page
                onDone = {
                    // Guide Books reads GuideBrowseSelection: copy the new choice into it
                    GuideBrowseSelection.selectFrom(PaperBrowseSelection.state.value)
                    navController.popBackStack(target, false)
                },
                title = browseTitle(target),
                hint = when (target) {
                    Routes.MARKING_SCHEMES -> "Marking schemes will be shown for these subjects only"
                    Routes.GUIDE_BOOKS -> "Guide books will be shown for these subjects only"
                    Routes.QUIZZES -> "Quizzes will be shown for these subjects only"
                    else -> "Papers will be shown for these subjects only"
                }
            )
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
                // Guests: the same stream + subjects as Papers / Marking Schemes (chosen once, this session only)
                val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                if (browse.hasSelection) {
                    // The subject list of Guide Books reads GuideBrowseSelection, so keep it the same
                    LaunchedEffect(browse) { GuideBrowseSelection.selectFrom(browse) }
                    GuideBookCategoriesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCategory = { category ->
                            navController.navigate(Routes.guideBooksRoute(category))
                        },
                        selection = GuideBrowseState(
                            streamId = browse.streamId,
                            streamName = browse.streamName,
                            subjects = browse.subjects
                        ),
                        onChangeSelection = {
                            navController.navigate(Routes.guestStreamsRoute(Routes.GUIDE_BOOKS))
                        }
                    )
                } else {
                    StreamSelectScreen(
                        onBack = { navController.popBackStack() },
                        onStreamSelected = { streamId ->
                            navController.navigate(Routes.guestSubjectsRoute(Routes.GUIDE_BOOKS, streamId))
                        },
                        title = "Guide Books"
                    )
                }
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
                // Guests: the books page shows the chosen subjects as chips (no "All"), only those with books
                GuideBooksScreen(onBack = { navController.popBackStack() })
            }
        }

        composable(Routes.QUIZZES) {
            if (session.isStudent) {
                // Students: the stream + subjects chosen at sign-up are used (same as Papers).
                // Only the student's own subjects are shown, with a "Change" button.
                StudentStudyGate(
                    title = "Quizzes",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    QuizSubjectSelectScreen(
                        onBack = { navController.popBackStack() },
                        onChangeSelection = { navController.navigate(Routes.STUDY_EDIT) },
                        onSubjectSelected = { subjectId ->
                            navController.navigate(Routes.quizzesBySubjectRoute(subjectId))
                        }
                    )
                }
            } else {
                // Guests: choose stream + subjects first (this session only), then only those subjects' quizzes
                val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                if (browse.hasSelection) {
                    QuizListScreen(
                        onBack = { navController.popBackStack() },
                        onStartQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) },
                        onChangeSelection = {
                            navController.navigate(Routes.guestStreamsRoute(Routes.QUIZZES))
                        }
                    )
                } else {
                    StreamSelectScreen(
                        onBack = { navController.popBackStack() },
                        onStreamSelected = { streamId ->
                            navController.navigate(Routes.guestSubjectsRoute(Routes.QUIZZES, streamId))
                        },
                        title = "Quizzes"
                    )
                }
            }
        }
        composable(
            route = Routes.QUIZZES_BY_SUBJECT,
            arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
        ) {
            // Student version: shows only the chosen subject's quizzes
            StudentQuizListScreen(
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
        // My teacher: 1) teacher list (free first, then paid)
        composable(Routes.TEACHERS) {
            val openTeacher: (String) -> Unit = { teacherId ->
                navController.navigate(Routes.teacherMaterialsRoute(teacherId))
            }
            if (session.isStudent) {
                // Students: the stream + subjects chosen at sign-up decide which teachers are shown
                StudentStudyGate(
                    title = "My teacher",
                    onBack = { navController.popBackStack() },
                    onChange = { navController.navigate(Routes.STUDY_EDIT) }
                ) {
                    val browse by PaperBrowseSelection.state.collectAsStateWithLifecycle()
                    TeacherListScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTeacher = openTeacher,
                        selection = browse,
                        onChangeSelection = { navController.navigate(Routes.STUDY_EDIT) }
                    )
                }
            } else {
                TeacherListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTeacher = openTeacher
                )
            }
        }
        // 2) the chosen teacher's materials (paid teacher: fee + Pay button)
        composable(
            route = Routes.TEACHER_MATERIALS,
            arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
        ) { entry ->
            val teacherId = entry.arguments?.getString("teacherId").orEmpty()
            TeacherMaterialsScreen(
                teacherId = teacherId,
                onBack = { navController.popBackStack() },
                onPay = { navController.navigate(Routes.teacherPaymentRoute(teacherId)) }
            )
        }
        // 3) payment dashboard -> confirm -> success -> back to the (now unlocked) materials
        composable(
            route = Routes.TEACHER_PAYMENT,
            arguments = listOf(navArgument("teacherId") { type = NavType.StringType })
        ) { entry ->
            val teacherId = entry.arguments?.getString("teacherId").orEmpty()
            TeacherPaymentScreen(
                teacherId = teacherId,
                onBack = { navController.popBackStack() },
                onPaid = {
                    // Remove the old materials screen + payment screen, then open a fresh materials screen
                    navController.navigate(Routes.teacherMaterialsRoute(teacherId)) {
                        popUpTo(Routes.TEACHER_MATERIALS) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TIMETABLE) {
            TimetableScreen(
                onBack = { navController.popBackStack() },
                onSetup = { navController.navigate(Routes.TIMETABLE_SETUP) },
                onOpenQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) }
            )
        }
        composable(Routes.TIMETABLE_SETUP) {
            PlannerSetupScreen(
                onBack = { navController.popBackStack() },
                onChooseSubjects = { navController.navigate(Routes.STUDY_EDIT) },
                onFinished = { navController.popBackStack() }
            )
        }
        composable(Routes.REMINDERS) {
            RemindersScreen(
                onBack = { navController.popBackStack() },
                onOpenTimetable = { navController.navigate(Routes.TIMETABLE) }
            )
        }
        // Files the student saved inside the app (open without internet)
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ANALYTICS) {
            if (session.isStudent) {
                StudyProgressScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQuizzes = { navController.navigate(Routes.QUIZZES) },
                    onOpenQuiz = { quizId -> navController.navigate(Routes.quizRoute(quizId)) }
                )
            } else {
                FeaturePlaceholderScreen(title = "Study Progress", onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun browseTitle(target: String): String = when (target) {
    Routes.MARKING_SCHEMES -> "Marking Schemes"
    Routes.GUIDE_BOOKS -> "Guide Books"
    Routes.QUIZZES -> "Quizzes"
    else -> "Papers"
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