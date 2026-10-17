package lk.nenasayura.app.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import lk.nenasayura.app.ui.common.FileViewerActivity
import java.io.File

/** Opens or downloads a document (paper, marking scheme, guide book...) from its URL. */
object FileActions {

    /**
     * Opens the file inside the app (PDF pages / image viewer).
     * If the file was downloaded before, the saved copy is used, so it also works offline.
     * [title] is shown in the top bar of the viewer.
     */
    fun openFile(context: Context, url: String, title: String = "") {
        if (url.isBlank()) {
            toast(context, "The file link is missing.")
            return
        }
        val intent = FileViewerActivity.intent(context, url, title)
        // Only needed when the context is not an Activity
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Opens the file in another app (PDF app or the browser). Used for file types the viewer can't show. */
    fun openExternally(context: Context, url: String) {
        if (url.isBlank()) {
            toast(context, "The file link is missing.")
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            toast(context, "No app found to open this file.")
        }
    }

    /**
     * Saves the file INSIDE the app (see [DownloadStore]) so it can be opened later without
     * internet. The student finds it again on the Downloads screen.
     * [source] is the copy the viewer already has on the disk (avoids downloading twice).
     */
    fun downloadFile(context: Context, url: String, title: String, source: File? = null) {
        if (url.isBlank()) {
            toast(context, "The file link is missing.")
            return
        }
        // A YouTube link is a video page, not a file
        if (YouTubeLinks.extractId(url) != null) {
            toast(context, "Videos can't be downloaded. Open it to watch online.")
            return
        }
        if (DownloadStore.isSaved(context, url)) {
            toast(context, "Already saved. Find it in Downloads.")
            return
        }
        toast(context, "Downloading...")
        DownloadStore.save(context, url, title, source) { error ->
            toast(
                context,
                error ?: "Saved in the app. Find it in Downloads (works offline)."
            )
        }
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}