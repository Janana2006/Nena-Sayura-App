package lk.nenasayura.app

import android.content.Intent
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.session.AppSession
import lk.nenasayura.app.core.session.FirebaseSessionProvider
import lk.nenasayura.app.screens.CheckScreen
import lk.nenasayura.app.screens.ForgotPasswordScreen
import lk.nenasayura.app.screens.HomeScreen
import lk.nenasayura.app.screens.LoginScreen
import lk.nenasayura.app.screens.RegisterScreen
import lk.nenasayura.app.screens.ResetPasswordScreen
import lk.nenasayura.app.screens.RoleSelectScreen
import lk.nenasayura.app.screens.SetupScreen
import lk.nenasayura.app.ui.admin.AdminHomeScreen
import lk.nenasayura.app.ui.common.applyActiveRole
import lk.nenasayura.app.ui.teacher.TeacherHomeScreen
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme
import lk.nenasayura.app.viewmodel.AuthViewModel
import lk.nenasayura.app.viewmodel.SetupViewModel

class MainActivity : ComponentActivity() {

    companion object {
        // Guest screen eken "Log in" click kara enawa nam true
        const val EXTRA_FROM_GUEST = "from_guest"
    }

    // reset link eken awa code eka. State ekak widiyata thiyenne, app eka open ekama
    // thiyenakota link ekak awath screen eka ResetPassword ekata wenas wenna
    private var resetCode by mutableStateOf<String?>(null)

    // reset link ekak awama hama parakma wenas wenawa (same code eka aye awath screen eka wenas wenna)
    private var resetNonce by mutableIntStateOf(0)

    // Reset password link eken app eka open una nam oobCode eka gannawa
    private fun resetCodeFrom(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.path?.startsWith("/__/auth/action") != true) return null
        if (uri.getQueryParameter("mode") != "resetPassword") return null
        return uri.getQueryParameter("oobCode")
    }

    // app eka already open wela thiyenakota link ekak awoth meka run wenawa
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("ResetLink", "onNewIntent mode=${intent.data?.getQueryParameter("mode")} hasCode=${resetCodeFrom(intent) != null}")
        setIntent(intent)
        resetCodeFrom(intent)?.let {
            resetCode = it
            resetNonce++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fromGuest = intent.getBooleanExtra(EXTRA_FROM_GUEST, false)
        Log.d("ResetLink", "onCreate mode=${intent.data?.getQueryParameter("mode")} hasCode=${resetCodeFrom(intent) != null}")
        resetCode = resetCodeFrom(intent)

        setContent {
            NenaSayuraAppTheme {
                val authViewModel: AuthViewModel = viewModel()
                val setupViewModel: SetupViewModel = viewModel()

                // Reset link eken awoth ResetPassword, kalin login wela nam Check, nathnam Login
                var selectScreen by remember {
                    mutableStateOf(
                        when {
                            resetCode != null -> "ResetPassword"
                            authViewModel.isLoggedIn() -> "Check"
                            else -> "Login"
                        }
                    )
                }

                // app eka open wela thiyenakota aluth reset link ekak awoth screen eka wenas karanawa
                LaunchedEffect(resetCode, resetNonce) {
                    if (resetCode != null) selectScreen = "ResetPassword"
                }

                var prefillEmail by remember { mutableStateOf("") }
                var prefillPassword by remember { mutableStateOf("") }

                val navigate: (String) -> Unit = { screen ->
                    if (screen == "Guest") {
                        // Guest: GuestHomeActivity (ui/guest/GuestHomeScreen) open karanna
                        AppSession.provider.signOut()
                        startActivity(Intent(this@MainActivity, GuestHomeActivity::class.java))
                        finish()
                    } else if (screen == "Home" || screen == "TeacherHome") {
                        // Login (normal ho Google) una passe role eka balala home eka decide karanawa:
                        //  teacher -> Teacher Home, student -> Student Home (GuestHomeActivity)
                        authViewModel.loadProfile { result ->
                            val profile = result.getOrNull()
                            if (profile != null) {
                                if (profile.role == "teacher") {
                                    FirebaseSessionProvider.signInTeacher(
                                        uid = profile.uid,
                                        name = profile.name,
                                        email = profile.email
                                    )
                                    selectScreen = "TeacherHome"
                                } else {
                                    FirebaseSessionProvider.signInStudent(
                                        uid = profile.uid,
                                        name = profile.name,
                                        email = profile.email
                                    )
                                    startActivity(Intent(this@MainActivity, GuestHomeActivity::class.java))
                                    finish()
                                }
                            } else {
                                selectScreen = "Login"
                            }
                        }
                    } else {
                        if (screen == "Check") {
                            prefillEmail = ""
                            prefillPassword = ""
                        }
                        selectScreen = screen
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)

                    when (selectScreen) {
                        "Login" -> LoginScreen(
                            modifier = modifier,
                            viewModel = authViewModel,
                            initialEmail = prefillEmail,
                            initialPassword = prefillPassword,
                            showGuestOption = !fromGuest,
                            onNavigate = navigate
                        )
                        "Register" -> RegisterScreen(
                            modifier = modifier,
                            viewModel = authViewModel,
                            onNavigate = navigate,
                            onRegistered = { e, p ->
                                prefillEmail = e
                                prefillPassword = p
                                selectScreen = "Login"
                            }
                        )
                        "Forgot" -> ForgotPasswordScreen(modifier, authViewModel, navigate)
                        // key(resetNonce) - aluth link ekak awoth screen eka mulinma aye check karanawa
                        "ResetPassword" -> key(resetNonce) {
                            ResetPasswordScreen(
                                modifier = modifier,
                                viewModel = authViewModel,
                                code = resetCode ?: "",
                                onNavigate = navigate
                            )
                        }
                        "Check" -> CheckScreen(modifier, authViewModel, navigate)

                        // NEW: Google wage aluth user: Student da Teacher da kiyala ahanawa
                        "RoleSelect" -> RoleSelectScreen(
                            modifier = modifier,
                            viewModel = authViewModel,
                            onContinue = { role ->
                                selectScreen = if (role == "teacher") "TeacherSetup" else "StudentSetup"
                            },
                            onLogout = {
                                authViewModel.logout()
                                selectScreen = "Login"
                            }
                        )

                        // Teacher: stream + subject 1k
                        "TeacherSetup" -> SetupScreen(
                            modifier = modifier,
                            viewModel = setupViewModel,
                            role = "teacher",
                            onDone = { navigate("TeacherHome") },
                            onLogout = {
                                authViewModel.logout()
                                selectScreen = "Login"
                            }
                        )

                        // Student: stream + subjects 3k
                        "StudentSetup" -> SetupScreen(
                            modifier = modifier,
                            viewModel = setupViewModel,
                            role = "student",
                            onDone = { navigate("Home") },
                            onLogout = {
                                authViewModel.logout()
                                selectScreen = "Login"
                            }
                        )

                        // Teacher: real Teacher Home (ui/teacher)
                        "TeacherHome" -> TeacherHomeScreen(
                            // Teacher -> Student dashboard: student home lives in GuestHomeActivity
                            onRoleChanged = { role ->
                                applyActiveRole(role)
                                if (role == "student") {
                                    startActivity(Intent(this@MainActivity, GuestHomeActivity::class.java))
                                    finish()
                                }
                            },
                            onSignOut = {
                                AppSession.provider.signOut()
                                authViewModel.logout()
                                selectScreen = "Login"
                            }
                        )

                        // Admin: Admin Dashboard (ui/admin)
                        "AdminHome" -> AdminHomeScreen(
                            onSignOut = {
                                AppSession.provider.signOut()
                                authViewModel.logout()
                                selectScreen = "Login"
                            }
                        )
                        "Home" -> HomeScreen(modifier, authViewModel, navigate)
                    }
                }
            }
        }
    }
}