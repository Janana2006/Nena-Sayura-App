package lk.nenasayura.app.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object CloudinaryUploader {

    private const val CLOUD_NAME = "bz7hbhv3"
    private const val UPLOAD_PRESET = "nena_sayura_app"
    private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/auto/upload"

    private val client = OkHttpClient()

    /** Uploads a PDF or image to Cloudinary and returns its https link. */
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
        fileName: String,
        folder: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Could not read the selected file."))

            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            post(bytes, mime, fileName, folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Uploads bytes that are already in memory (for example a shrunk profile photo). */
    suspend fun uploadBytes(
        bytes: ByteArray,
        fileName: String,
        mime: String,
        folder: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            post(bytes, mime, fileName, folder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sends the file to Cloudinary. Call it from a background thread.
    private fun post(bytes: ByteArray, mime: String, fileName: String, folder: String): Result<String> {
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("file", fileName, body)
        if (folder.isNotBlank()) builder.addFormDataPart("folder", folder)

        val request = Request.Builder().url(UPLOAD_URL).post(builder.build()).build()

        val response = client.newCall(request).execute()
        val text = response.body?.string().orEmpty()

        return if (response.isSuccessful) {
            val url = JSONObject(text).optString("secure_url")
            if (url.isNotBlank()) Result.success(url)
            else Result.failure(Exception("Cloudinary did not return a link."))
        } else {
            val message = try {
                JSONObject(text).optJSONObject("error")?.optString("message") ?: text
            } catch (e: Exception) {
                text
            }
            Result.failure(Exception(message.ifBlank { "Upload failed (HTTP ${response.code})" }))
        }
    }

    /** Kept so older code still compiles. */
    suspend fun uploadPdf(context: Context, uri: Uri, fileName: String): Result<String> =
        uploadFile(context, uri, fileName)
}