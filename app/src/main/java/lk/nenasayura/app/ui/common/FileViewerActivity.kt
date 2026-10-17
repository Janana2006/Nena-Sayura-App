package lk.nenasayura.app.ui.common

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaSayuraAppTheme
import lk.nenasayura.app.util.FileActions
import lk.nenasayura.app.util.YouTubeLinks
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Shows a paper / marking scheme / guide book inside the app (no browser).
 * PDFs are drawn page by page, images can be pinch-zoomed.
 * Start it with [FileActions.openFile]. It downloads the file once and keeps it in the cache.
 */
class FileViewerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"

        fun intent(context: Context, url: String, title: String): Intent =
            Intent(context, FileViewerActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Document" }

        setContent {
            NenaSayuraAppTheme {
                FileViewerScreen(url = url, title = title, onBack = { finish() })
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------------------------

private sealed interface ViewerState {
    data class Loading(val percent: Int?) : ViewerState
    data class Failed(val message: String) : ViewerState
    data object Unsupported : ViewerState
    data class Pdf(val file: File) : ViewerState
    data class Photo(val file: File) : ViewerState
}

private enum class FileKind { PDF, IMAGE, OTHER }

private val zoomSteps = listOf(1f, 1.5f, 2f, 2.5f)

@Composable
private fun FileViewerScreen(url: String, title: String, onBack: () -> Unit) {
    // A YouTube link is not a file: play the video here instead of trying to download it
    val videoId = remember(url) { YouTubeLinks.extractId(url) }
    if (videoId != null) {
        VideoViewerScreen(videoId = videoId, title = title, onBack = onBack)
        return
    }

    val context = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ViewerState>(ViewerState.Loading(null)) }
    var zoomIndex by remember { mutableIntStateOf(0) }

    // Download (or take from cache), then find out if it is a PDF or an image
    LaunchedEffect(url, attempt) {
        state = ViewerState.Loading(null)
        state = try {
            if (url.isBlank()) throw IOException("The file link is missing.")
            val file = downloadToCache(context, url) { percent ->
                state = ViewerState.Loading(percent)
            }
            when (detectKind(file)) {
                FileKind.PDF -> ViewerState.Pdf(file)
                FileKind.IMAGE -> ViewerState.Photo(file)
                FileKind.OTHER -> ViewerState.Unsupported
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ViewerState.Failed(
                e.message?.takeIf { it.isNotBlank() }
                    ?: "Could not load the file. Check your internet connection."
            )
        }
    }

    Scaffold(
        topBar = {
            NenaTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (state is ViewerState.Pdf) {
                        IconButton(
                            onClick = { if (zoomIndex > 0) zoomIndex-- },
                            enabled = zoomIndex > 0
                        ) { Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out") }
                        IconButton(
                            onClick = { if (zoomIndex < zoomSteps.lastIndex) zoomIndex++ },
                            enabled = zoomIndex < zoomSteps.lastIndex
                        ) { Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in") }
                    }
                    IconButton(onClick = { FileActions.downloadFile(context, url, title) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                is ViewerState.Loading -> LoadingContent(s.percent)

                is ViewerState.Failed -> MessageContent(
                    message = s.message,
                    buttonLabel = "Try again",
                    onButton = { attempt++ }
                )

                ViewerState.Unsupported -> MessageContent(
                    message = "This file type can't be previewed inside the app.",
                    buttonLabel = "Open in another app",
                    onButton = { FileActions.openExternally(context, url) }
                )

                is ViewerState.Pdf -> PdfViewer(file = s.file, zoom = zoomSteps[zoomIndex])
                is ViewerState.Photo -> PhotoViewer(file = s.file)
            }
        }
    }
}

/** Plays a YouTube video inside the app (16:9 in portrait, full screen in landscape). */
@Composable
private fun VideoViewerScreen(videoId: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // true = show the normal YouTube page (for videos that cannot be embedded)
    var watchPage by remember(videoId) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NenaTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    // open the same video in the YouTube app
                    IconButton(
                        onClick = {
                            FileActions.openExternally(context, YouTubeLinks.watchUrl(videoId))
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in YouTube")
                    }
                }
            )
        },
        containerColor = Color(0xFF0E1116)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                Text(
                    text = title,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (!watchPage) {
                    TextButton(
                        onClick = { watchPage = true },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Video not playing? Try the YouTube page", color = Color(0xFF8AB4F8))
                    }
                } else {
                    Text(
                        text = "Playing with the YouTube page. You can also open it in the YouTube app with the icon at the top.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(percent: Int?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (percent != null) {
            CircularProgressIndicator(progress = { percent / 100f }, color = NenaPrimary)
        } else {
            CircularProgressIndicator(color = NenaPrimary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (percent != null) "Loading... $percent%" else "Loading...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageContent(message: String, buttonLabel: String, onButton: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onButton) { Text(buttonLabel) }
    }
}

// ---------------------------------------------------------------------------------------------
// PDF
// ---------------------------------------------------------------------------------------------

@Composable
private fun PdfViewer(file: File, zoom: Float) {
    val doc = remember(file) { runCatching { PdfDoc(file) }.getOrNull() }
    DisposableEffect(doc) { onDispose { doc?.close() } }

    if (doc == null) {
        MessageContent(
            message = "This PDF could not be opened.",
            buttonLabel = "OK",
            onButton = {}
        )
        return
    }

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val pageWidth = screenWidth * zoom
    // Sharp enough to read, but not so big that the phone runs out of memory
    val pageWidthPx = with(density) { pageWidth.roundToPx() }.coerceAtMost(2000)
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE4E7EE))
    ) {
        // The horizontal scroll lets you pan sideways when zoomed in
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(pageWidth)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(doc.pageCount) { index ->
                    PdfPage(doc = doc, index = index, widthPx = pageWidthPx)
                }
            }
        }

        // "3 / 12" page indicator
        Text(
            text = "${listState.firstVisibleItemIndex + 1} / ${doc.pageCount}",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .background(Color(0xCC1B1B1F), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
private fun PdfPage(doc: PdfDoc, index: Int, widthPx: Int) {
    var bitmap by remember(doc, index, widthPx) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(doc, index, widthPx) { mutableStateOf(false) }

    // Each page is drawn only when it scrolls into view
    LaunchedEffect(doc, index, widthPx) {
        try {
            bitmap = doc.render(index, widthPx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
    }

    val image = bitmap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .shadow(2.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
                contentAlignment = Alignment.Center
            ) {
                if (failed) {
                    Text("Could not load page ${index + 1}", color = Color.Gray)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = NenaPrimary
                    )
                }
            }
        }
    }
}

/** Small wrapper around Android's PdfRenderer. It can draw only one page at a time, so a Mutex is used. */
private class PdfDoc(file: File) {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val mutex = Mutex()

    val pageCount: Int = renderer.pageCount

    suspend fun render(index: Int, widthPx: Int): Bitmap = withContext(Dispatchers.Default) {
        mutex.withLock {
            val page = renderer.openPage(index)
            try {
                val height = (widthPx.toFloat() * page.height / page.width).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)   // PDF pages have no background of their own
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        }
    }

    fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

// ---------------------------------------------------------------------------------------------
// Images (pinch to zoom, double tap to zoom in / out)
// ---------------------------------------------------------------------------------------------

@Composable
private fun PhotoViewer(file: File) {
    val bitmap by produceState<Bitmap?>(initialValue = null, file) {
        value = withContext(Dispatchers.IO) { decodeSampled(file, 2048) }
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.5f
                    offset = Offset.Zero
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

private fun decodeSampled(file: File, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
    return BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sample }
    )
}

// ---------------------------------------------------------------------------------------------
// Download + cache
// ---------------------------------------------------------------------------------------------

private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
}

/** Downloads [url] into the app cache (only the first time) and returns the file. */
private suspend fun downloadToCache(
    context: Context,
    url: String,
    onProgress: (Int?) -> Unit
): File = withContext(Dispatchers.IO) {
    val dir = File(context.cacheDir, "viewer").apply { mkdirs() }
    val target = File(dir, sha256(url).take(24))
    if (target.exists() && target.length() > 0) return@withContext target

    val request = Request.Builder().url(url).build()
    httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Could not download the file (error ${response.code}).")
        }
        val body = response.body ?: throw IOException("The server sent an empty file.")
        val total = body.contentLength()
        val temp = File(dir, target.name + ".part")

        body.byteStream().use { input ->
            temp.outputStream().use { output ->
                val buffer = ByteArray(16 * 1024)
                var done = 0L
                var lastPercent = -1
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    done += read
                    if (total > 0) {
                        val percent = (done * 100 / total).toInt()
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
            }
        }
        if (!temp.renameTo(target)) throw IOException("Could not save the file.")
    }
    target
}

/** Looks at the first bytes of the file: "%PDF" is a PDF, anything Android can decode is an image. */
private fun detectKind(file: File): FileKind {
    val head = ByteArray(5)
    val read = file.inputStream().use { it.read(head) }
    if (read >= 4 && String(head, 0, 4, Charsets.ISO_8859_1) == "%PDF") return FileKind.PDF

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    return if (bounds.outWidth > 0) FileKind.IMAGE else FileKind.OTHER
}

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }