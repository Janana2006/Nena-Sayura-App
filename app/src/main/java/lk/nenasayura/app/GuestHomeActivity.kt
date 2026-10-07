package lk.nenasayura.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import lk.nenasayura.app.core.session.DevSessionProvider
import lk.nenasayura.app.core.session.UserSession
import lk.nenasayura.app.navigation.AppNavGraph
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme

/**
 * Entry point of the Guest + Student + Teacher modules.
 * Hosts the navigation graph.
 */
class GuestHomeActivity : ComponentActivity() {

    // TEMPORARY: set to true to open the app directly as a test teacher.
    // Set to false (or delete the block below) once the real login is ready.
    private val useTestTeacher = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (useTestTeacher && DevSessionProvider.session.value is UserSession.Guest) {
            DevSessionProvider.signInAsTestTeacher()
        }

        // Transparent status bar with light icons, because our headers are dark blue.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        setContent {
            NenaSayuraAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}