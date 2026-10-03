package lk.nenasayura.app.util



import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/** Opens or downloads a document (paper, marking scheme, guide book...) from its URL. */
object FileActions {

    /** Opens the file in a PDF app or the browser. */
    fun openFile(context: Context, url: String) {
        if (url.isBlank()) {
            toast(context, "The file link is missing.")
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast(context, "No app found to open this file.")
        }
    }

    /** Saves the file to Downloads/NenaSayura using the system DownloadManager. */
    fun downloadFile(context: Context, url: String, title: String) {
        if (url.isBlank()) {
            toast(context, "The file link is missing.")
            return
        }
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(title)
                .setDescription("Nena Sayura")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "NenaSayura/${buildFileName(title, url)}"
                )
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            toast(context, "Download started. Check your notifications.")
        } catch (e: Exception) {
            toast(context, "Could not start the download.")
        }
    }

    // "Chemistry 2023 Paper I" + ".../x.pdf?alt=media" -> "Chemistry_2023_Paper_I.pdf"
    private fun buildFileName(title: String, url: String): String {
        val safeTitle = title.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "document" }
        val lastSegment = Uri.parse(url).lastPathSegment ?: ""
        val extension = lastSegment.substringAfterLast('.', "pdf")
            .takeIf { it.length in 2..5 && it.all { c -> c.isLetterOrDigit() } }
            ?: "pdf"
        return "$safeTitle.$extension"
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}