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

    suspend fun uploadPdf(
        context: Context,
        uri: Uri,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return@withContext Result.failure(Exception("Could not read PDF bytes from Uri."))

            val mediaType = "application/pdf".toMediaTypeOrNull()
            val fileRequestBody = bytes.toRequestBody(mediaType)

            val safeFileName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .addFormDataPart("file", safeFileName, fileRequestBody)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBodyString)
                val secureUrl = jsonResponse.optString("secure_url")
                if (secureUrl.isNotBlank()) {
                    Result.success(secureUrl)
                } else {
                    Result.failure(Exception("Cloudinary response missing 'secure_url'."))
                }
            } else {
                val errorMessage = if (responseBodyString.isNotBlank()) {
                    try {
                        val jsonError = JSONObject(responseBodyString)
                        val errorObject = jsonError.optJSONObject("error")
                        errorObject?.optString("message") ?: responseBodyString
                    } catch (e: Exception) {
                        responseBodyString
                    }
                } else {
                    "Upload failed with HTTP status ${response.code}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
