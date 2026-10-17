package lk.nenasayura.app.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** One file the student saved inside the app. */
data class DownloadedFile(
    val id: String,
    val title: String,
    /** The original web link. The viewer uses it to find the saved copy. */
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val savedAtMillis: Long
)

/**
 * Files saved INSIDE the app (private storage, not the phone's Downloads folder):
 *
 *   files/downloads/<id>.<ext>   the saved files
 *   files/downloads/index.json   the list shown on the Downloads screen
 */
object DownloadStore {

    private val _items = MutableStateFlow<List<DownloadedFile>>(emptyList())
    /** Newest first. */
    val items: StateFlow<List<DownloadedFile>> = _items.asStateFlow()

    private val _busy = MutableStateFlow<Set<String>>(emptySet())
    /** Ids of the files that are downloading right now. */
    val busy: StateFlow<Set<String>> = _busy.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var loaded = false

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ---------------- Reading ----------------

    /** Reads the saved list from the disk (only the first time). Safe to call many times. */
    fun load(context: Context) {
        synchronized(lock) {
            if (loaded) return
            loaded = true
            val list = readIndex(context)
                .filter { File(dir(context), it.fileName).let { f -> f.exists() && f.length() > 0 } }
            _items.value = list.sortedByDescending { it.savedAtMillis }
        }
    }

    fun idFor(url: String): String = sha256(url).take(24)

    fun isSaved(context: Context, url: String): Boolean = localFile(context, url) != null

    /** The saved copy of [url], or null when it was not downloaded. */
    fun localFile(context: Context, url: String): File? {
        load(context)
        val item = _items.value.firstOrNull { it.id == idFor(url) } ?: return null
        return File(dir(context), item.fileName).takeIf { it.exists() && it.length() > 0 }
    }

    // ---------------- Saving ----------------

    /**
     * Saves [url] into the app. If the viewer already has the file on the disk, pass it as
     * [source] so it is copied instead of downloaded again.
     * [onResult] is called on the main thread: null = saved, otherwise the error message.
     */
    fun save(
        context: Context,
        url: String,
        title: String,
        source: File? = null,
        onResult: (error: String?) -> Unit
    ) {
        val app = context.applicationContext
        val id = idFor(url)
        load(app)

        if (isSaved(app, url)) {
            onResult(null)
            return
        }
        if (id in _busy.value) return          // already downloading

        _busy.update { it + id }
        scope.launch {
            val error = try {
                val target = File(dir(app), id + "." + extensionOf(title, url))
                val temp = File(dir(app), "$id.part")
                if (source != null && source.exists() && source.length() > 0) {
                    source.copyTo(temp, overwrite = true)
                } else {
                    downloadTo(url, temp)
                }
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) throw IOException("Could not save the file.")

                val item = DownloadedFile(
                    id = id,
                    title = title.ifBlank { "Document" },
                    url = url,
                    fileName = target.name,
                    sizeBytes = target.length(),
                    savedAtMillis = System.currentTimeMillis()
                )
                synchronized(lock) {
                    _items.value = (_items.value.filterNot { it.id == id } + item)
                        .sortedByDescending { it.savedAtMillis }
                    writeIndex(app, _items.value)
                }
                null
            } catch (e: Exception) {
                File(dir(app), "$id.part").delete()
                friendly(e)
            } finally {
                _busy.update { it - id }
            }
            main.post { onResult(error) }
        }
    }

    // ---------------- Deleting ----------------

    fun delete(context: Context, id: String) {
        val app = context.applicationContext
        load(app)
        scope.launch {
            synchronized(lock) {
                _items.value.firstOrNull { it.id == id }?.let { File(dir(app), it.fileName).delete() }
                _items.value = _items.value.filterNot { it.id == id }
                writeIndex(app, _items.value)
            }
        }
    }

    // ---------------- Internals ----------------

    private fun dir(context: Context): File =
        File(context.applicationContext.filesDir, "downloads").apply { mkdirs() }

    private fun downloadTo(url: String, temp: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Could not download the file (error ${response.code}).")
            }
            val body = response.body ?: throw IOException("The server sent an empty file.")
            body.byteStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is java.net.UnknownHostException,
        is java.net.SocketTimeoutException,
        is java.net.ConnectException ->
            "No internet connection. Connect and try again."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Could not save the file."
    }

    // ".../x.pdf?alt=media" -> "pdf"
    private fun extensionOf(title: String, url: String): String {
        val last = Uri.parse(url).lastPathSegment ?: ""
        return last.substringAfterLast('.', "pdf")
            .takeIf { it.length in 2..5 && it.all { c -> c.isLetterOrDigit() } }
            ?: "pdf"
    }

    private fun readIndex(context: Context): List<DownloadedFile> = try {
        val file = File(dir(context), "index.json")
        if (!file.exists()) emptyList() else {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                DownloadedFile(
                    id = o.getString("id"),
                    title = o.optString("title", "Document"),
                    url = o.getString("url"),
                    fileName = o.getString("fileName"),
                    sizeBytes = o.optLong("sizeBytes", 0L),
                    savedAtMillis = o.optLong("savedAtMillis", 0L)
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun writeIndex(context: Context, list: List<DownloadedFile>) {
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("url", it.url)
                    .put("fileName", it.fileName)
                    .put("sizeBytes", it.sizeBytes)
                    .put("savedAtMillis", it.savedAtMillis)
            )
        }
        File(dir(context), "index.json").writeText(array.toString())
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
}