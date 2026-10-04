package lk.nenasayura.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
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
import lk.nenasayura.app.screens.SetupScreen
import lk.nenasayura.app.screens.TeacherHomeScreen
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme
import lk.nenasayura.app.viewmodel.AuthViewModel
import lk.nenasayura.app.viewmodel.SetupViewModel

class MainActivity : ComponentActivity() {

    companion object {
        // Guest screen eken "Log in" click kara enawa nam true
        const val EXTRA_FROM_GUEST = "from_guest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fromGuest = intent.getBooleanExtra(EXTRA_FROM_GUEST, false)

        setContent {
            NenaSayuraAppTheme {
                val authViewModel: AuthViewModel = viewModel()
                val setupViewModel: SetupViewModel = viewModel()

                // kalin login wela nam role/setup check karanna "Check" ekata
                var selectScreen by remember {
                    mutableStateOf(if (authViewModel.isLoggedIn()) "Check" else "Login")
                }

                // Register una passe Login page ekata yawana email/password
                var prefillEmail by remember { mutableStateOf("") }
                var prefillPassword by remember { mutableStateOf("") }

                val navigate: (String) -> Unit = { screen ->
                    if (screen == "Guest") {
                        // Guest: GuestHomeActivity (ui/guest/GuestHomeScreen) open karanna
                        AppSession.provider.signOut()
                        startActivity(Intent(this@MainActivity, GuestHomeActivity::class.java))
                        finish()
                    } else if (screen == "Home") {
                        // Student login una: session eka set karala Student Home ekata yanna
                        authViewModel.loadProfile { result ->
                            val profile = result.getOrNull()
                            if (profile != null) {
                                FirebaseSessionProvider.signInStudent(
                                    uid = profile.uid,
                                    name = profile.name,
                                    email = profile.email
                                )
                                startActivity(Intent(this@MainActivity, GuestHomeActivity::class.java))
                                finish()
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
                        "Check" -> CheckScreen(modifier, authViewModel, navigate)

                        // Teacher: stream + subject 1k
                        "TeacherSetup" -> SetupScreen(
                            modifier = modifier,
                            viewModel = setupViewModel,
                            role = "teacher",
                            onDone = { selectScreen = "TeacherHome" },
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

                        "TeacherHome" -> TeacherHomeScreen(modifier, authViewModel, navigate)
                        "Home" -> HomeScreen(modifier, authViewModel, navigate)
                    }
                }
            }
        }
    }
}