package com.alieducational.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object AttachmentManager {

    private const val ATTACHMENTS_DIR_NAME = "attachments"
    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 85

    fun getAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, ATTACHMENTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Resolves a stored attachmentUri (which may be a relative path, filename, or absolute path)
     * to a valid File inside durable app storage, ensuring restored backups work on any device.
     */
    fun getAttachmentFile(context: Context, pathOrUri: String?): File? {
        if (pathOrUri.isNullOrBlank()) return null

        val fileName = File(pathOrUri).name
        val durableFile = File(getAttachmentsDir(context), fileName)
        if (durableFile.exists() && durableFile.length() > 0) {
            return durableFile
        }

        val directFile = File(pathOrUri)
        if (directFile.exists() && directFile.length() > 0) {
            return directFile
        }

        return null
    }

    /**
     * Creates a temporary file in cache and returns a FileProvider URI suitable for camera capture.
     */
    fun createTempCameraUri(context: Context): Pair<Uri, File> {
        val tempDir = File(context.cacheDir, "camera_temp")
        if (!tempDir.exists()) tempDir.mkdirs()

        val tempFile = File(tempDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        return Pair(uri, tempFile)
    }

    /**
     * Copies and normalizes an image from a content/file URI into durable app-managed attachment storage,
     * correcting EXIF orientation and keeping file size lightweight while retaining readability.
     * Returns the relative path "attachments/attachment_<UUID>.jpg".
     */
    suspend fun persistImageAttachment(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val attachmentsDir = getAttachmentsDir(context)
            val outputFileName = "attachment_${UUID.randomUUID()}.jpg"
            val outputFile = File(attachmentsDir, outputFileName)

            // 1. Check EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } catch (e: Exception) { android.util.Log.w("AttachmentManager", "Non-fatal attachment operation error: ${e.message}") }

            // 2. Decode bitmap bounds first to scale down if necessary
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var inSampleSize = 1
            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth > MAX_DIMENSION || origHeight > MAX_DIMENSION) {
                val halfHeight = origHeight / 2
                val halfWidth = origWidth / 2
                while ((halfHeight / inSampleSize) >= MAX_DIMENSION && (halfWidth / inSampleSize) >= MAX_DIMENSION) {
                    inSampleSize *= 2
                }
            }

            // 3. Decode bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decodedBitmap: Bitmap? = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
            val originalBitmap: Bitmap = decodedBitmap ?: return@withContext null

            // 4. Rotate bitmap if required by EXIF
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val finalBitmap: Bitmap = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(
                    originalBitmap,
                    0, 0,
                    originalBitmap.width,
                    originalBitmap.height,
                    matrix,
                    true
                )
                if (rotated != originalBitmap) originalBitmap.recycle()
                rotated
            } else {
                originalBitmap
            }

            // 5. Compress to durable JPEG file
            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            finalBitmap.recycle()

            "attachments/$outputFileName"
        } catch (e: Exception) {
            android.util.Log.w("AttachmentManager", "Failed to persist image attachment: ${e.message}", e)
            null
        }
    }

    /**
     * Safely deletes an attachment file from storage.
     */
    fun deleteAttachment(context: Context, pathOrUri: String?) {
        if (pathOrUri.isNullOrBlank()) return
        try {
            val file = getAttachmentFile(context, pathOrUri)
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (e: Exception) { android.util.Log.w("AttachmentManager", "Non-fatal attachment operation error: ${e.message}") }
    }
}
