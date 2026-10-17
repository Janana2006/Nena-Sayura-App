package lk.nenasayura.app.ui.common

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The YouTube player inside a WebView, so a video plays inside the app.
 * Used by the file viewer and by the student's video dialog.
 *
 * Some videos cannot be embedded (the owner switched embedding off: "Error 152"). Then
 *  - [onEmbedFailed] is called, and
 *  - when [watchPage] is true the normal YouTube mobile page is shown inside the same WebView,
 *    which plays those videos too.
 * The caller keeps the [watchPage] flag (set it in [onEmbedFailed] or from a button).
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun YouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    watchPage: Boolean = false,
    onEmbedFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentWatchPage by rememberUpdatedState(watchPage)
    val currentOnFailed by rememberUpdatedState(onEmbedFailed)

    val webView = remember(videoId) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    if (request?.isForMainFrame != true) return false
                    return if (currentWatchPage) {
                        // YouTube page mode: stay on YouTube / Google (cookie and consent pages)
                        val host = request.url.host.orEmpty()
                        !(host.endsWith("youtube.com") || host.endsWith("google.com") ||
                                host.endsWith("gstatic.com") || host.endsWith("googlevideo.com"))
                    } else {
                        // Player mode: a tap on the logo or a link must not open the site here
                        request.hasGesture()
                    }
                }
            }
            // The player tells us when YouTube refuses to play the video inside an app
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onPlayerError(code: Int) {
                    post { currentOnFailed() }
                }
            }, "Android")
            // The base URL gives the player the origin (referer) YouTube asks for when embedding
            loadDataWithBaseURL(
                "https://www.youtube.com",
                playerHtml(videoId),
                "text/html",
                "utf-8",
                null
            )
        }
    }

    // Switch to the normal YouTube page when asked
    LaunchedEffect(watchPage, webView) {
        if (watchPage) webView.loadUrl("https://m.youtube.com/watch?v=$videoId")
    }

    // Stop the video (and its sound) when the player is closed
    DisposableEffect(webView) {
        onDispose {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    AndroidView(modifier = modifier.background(Color.Black), factory = { webView })
}

// The IFrame API reports errors (video cannot be embedded, removed, private...) to the app
private fun playerHtml(videoId: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <style>
        html, body { margin: 0; padding: 0; height: 100%; background: #000; overflow: hidden; }
        #player { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
      </style>
    </head>
    <body>
      <div id="player"></div>
      <script src="https://www.youtube.com/iframe_api"></script>
      <script>
        function onYouTubeIframeAPIReady() {
          new YT.Player('player', {
            width: '100%',
            height: '100%',
            videoId: '$videoId',
            playerVars: {
              autoplay: 1, playsinline: 1, rel: 0, modestbranding: 1, fs: 0,
              origin: 'https://www.youtube.com'
            },
            events: {
              onError: function (e) { Android.onPlayerError(e.data); }
            }
          });
        }
      </script>
    </body>
    </html>
""".trimIndent()