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
import lk.nenasayura.app.core.browse.GuideBrowseSelection
import lk.nenasayura.app.core.browse.PaperBrowseSelection
import lk.nenasayura.app.navigation.AppNavGraph
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme

/**
 * Entry point of the Guest + Student module.
 * Hosts the navigation graph (Guest Home, resources, Student Home, ...).
 */
class GuestHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    override fun onDestroy() {
        // The Papers and Guide Books stream choices are temporary: forget them when the user leaves the app
        if (isFinishing) {
            PaperBrowseSelection.clear()
            GuideBrowseSelection.clear()
        }
        super.onDestroy()
    }
}