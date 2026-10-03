package lk.nenasayura.app.ui.student

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.MainActivity
import lk.nenasayura.app.screens.HomeScreen
import lk.nenasayura.app.viewmodel.AuthViewModel

@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel()
    HomeScreen(
        modifier = modifier,
        viewModel = viewModel,
        onNavigate = { _ ->
            viewModel.logout()
            onSignOut()
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
            (context as? android.app.Activity)?.finish()
        }
    )
}
