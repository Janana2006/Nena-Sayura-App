package lk.nenasayura.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Reads a picture from the camera or the gallery, turns it the right way up, shrinks it so
     * the longest side is at most [maxSide] pixels and returns it as JPEG bytes.
     * A phone photo of several MB becomes a few hundred KB, so the upload is quick.
     */
    fun compressForUpload(
        context: Context,
        uri: Uri,
        maxSide: Int = 1024,
        quality: Int = 85
    ): ByteArray {
        val resolver = context.contentResolver

        // 1) size of the picture, without loading it
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not read the picture." }

        // 2) load a smaller copy (power of 2) so a big photo does not fill the memory
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Could not read the picture.")

        // 3) camera photos are often saved sideways: read the rotation from the EXIF data
        val rotation = try {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }

        // 4) rotate + scale down, then save as JPEG
        val scale = minOf(1f, maxSide.toFloat() / maxOf(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            if (rotation != 0) postRotate(rotation.toFloat())
            if (scale < 1f) postScale(scale, scale)
        }
        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        return ByteArrayOutputStream().use { out ->
            result.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }
}