package lk.nenasayura.app.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lk.nenasayura.app.ui.theme.NenaPrimary
import okhttp3.OkHttpClient
import okhttp3.Request

private val imageClient = OkHttpClient()
private val imageCache = LruCache<String, Bitmap>(24)

/**
 * Loads a picture from an https link (for example a Cloudinary profile photo) and shows it.
 * Pictures already loaded are kept in memory. [placeholder] shows while loading or on failure.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {}
) {
    val bitmap by produceState<Bitmap?>(initialValue = imageCache.get(url), url) {
        value = imageCache.get(url)
        if (value == null && url.isNotBlank()) {
            val loaded = withContext(Dispatchers.IO) { runCatching { download(url) }.getOrNull() }
            if (loaded != null) {
                imageCache.put(url, loaded)
                value = loaded
            }
        }
    }
    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        placeholder()
    }
}

private fun download(url: String): Bitmap? {
    val response = imageClient.newCall(Request.Builder().url(url).build()).execute()
    response.use {
        if (!it.isSuccessful) return null
        val bytes = it.body?.bytes() ?: return null
        // load a smaller copy so a big picture does not fill the memory
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
}

/** Cloudinary can send a small square copy of the picture (faster than the full photo). */
private fun thumbnailUrl(url: String): String =
    if (url.contains("res.cloudinary.com") && url.contains("/upload/")) {
        url.replace("/upload/", "/upload/w_400,h_400,c_fill,g_auto/")
    } else {
        url
    }

/** Round profile picture: the photo when there is one, otherwise the first letter of the name. */
@Composable
fun ProfileAvatar(
    photoUrl: String,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White
) {
    val initial: @Composable () -> Unit = {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "S",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = NenaPrimary
        )
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            RemoteImage(
                url = thumbnailUrl(photoUrl),
                contentDescription = "Profile photo",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop,
                placeholder = initial
            )
        } else {
            initial()
        }
    }
}