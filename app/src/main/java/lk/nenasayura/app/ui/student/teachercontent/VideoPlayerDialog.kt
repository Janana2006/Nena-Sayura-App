package lk.nenasayura.app.ui.student.teachercontent

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import lk.nenasayura.app.ui.common.YouTubePlayer
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.util.YouTubeLinks

/**
 * Plays a teacher's YouTube video inside the app (no need to leave for the YouTube app).
 * The student can still tap "YouTube" to open the video there, for example when the owner
 * does not allow the video to be played inside other apps.
 */
@Composable
fun VideoPlayerDialog(
    title: String,
    description: String,
    videoUrl: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val videoId = YouTubeLinks.extractId(videoUrl)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // true = show the normal YouTube page (for videos that cannot be embedded)
    var watchPage by remember(videoUrl) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0E1116)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // top bar: close, title, open in YouTube
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(onClick = { FileActions.openFile(context, videoUrl, title) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YouTube", color = Color.White)
                    }
                }

                if (videoId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "This is not a YouTube link.",
                            color = Color.White
                        )
                    }
                } else {
                    // portrait: 16:9 player with the details below; landscape: the player fills the screen
                    Box(
                        modifier = if (landscape) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        }
                    ) {
                        YouTubePlayer(
                            videoId = videoId,
                            modifier = Modifier.fillMaxSize(),
                            watchPage = watchPage,
                            onEmbedFailed = { watchPage = true }
                        )
                    }

                    if (!landscape) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            if (!watchPage) {
                                TextButton(onClick = { watchPage = true }) {
                                    Text("Video not playing? Try the YouTube page", color = Color(0xFF8AB4F8))
                                }
                            } else {
                                Text(
                                    text = "Playing with the YouTube page. Tap YouTube at the top to open it in the YouTube app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}