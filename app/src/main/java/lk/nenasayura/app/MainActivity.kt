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
import lk.nenasayura.app.screens.ForgotPasswordScreen
import lk.nenasayura.app.screens.HomeScreen
import lk.nenasayura.app.screens.LoginScreen
import lk.nenasayura.app.screens.RegisterScreen
import lk.nenasayura.app.ui.teacher.TeacherHomeScreen
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme
import lk.nenasayura.app.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FROM_GUEST = "from_guest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NenaSayuraAppTheme {
                val authViewModel: AuthViewModel = viewModel()

                var selectScreen by remember {
                    mutableStateOf(if (authViewModel.isLoggedIn()) "Home" else "Login")
                }

                var prefillEmail by remember { mutableStateOf("") }
                var prefillPassword by remember { mutableStateOf("") }

                val navigate: (String) -> Unit = { screen ->
                    if (screen == "Guest") {
                        AppSession.provider.signOut()
                        startActivity(Intent(this@MainActivity, GuestHomeActivity::class.java))
                        finish()
                    } else if (screen == "Home" || screen == "TeacherHome") {
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
                        "TeacherHome" -> TeacherHomeScreen(
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
