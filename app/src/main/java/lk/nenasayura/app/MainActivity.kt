package lk.nenasayura.app

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
import lk.nenasayura.app.screens.ForgotPasswordScreen
import lk.nenasayura.app.screens.HomeScreen
import lk.nenasayura.app.screens.LoginScreen
import lk.nenasayura.app.screens.RegisterScreen
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme
import lk.nenasayura.app.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NenaSayuraAppTheme {
                val authViewModel: AuthViewModel = viewModel()

                // kalin login wela nam Home, naththam Login
                var selectScreen by remember {
                    mutableStateOf(if (authViewModel.isLoggedIn()) "Home" else "Login")
                }

                // Register una passe Login page ekata yawana email/password
                var prefillEmail by remember { mutableStateOf("") }
                var prefillPassword by remember { mutableStateOf("") }

                val navigate: (String) -> Unit = { screen ->
                    if (screen == "Home") {      // login una passe prefill clear karanna
                        prefillEmail = ""
                        prefillPassword = ""
                    }
                    selectScreen = screen
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
                        "Home" -> HomeScreen(modifier, authViewModel, navigate)
                    }
                }
            }
        }
    }
}