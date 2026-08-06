package com.prakash.pwall.data.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Stores an image in app-private storage so it can be read later by the live
 * wallpaper service without extra permissions. Images are copied from any
 * [Uri] (photo picker or Storage Access Framework). The directory and file
 * prefix are configurable so separate images (wallpaper vs. time-widget
 * container) can coexist without deleting each other.
 */
class ImageStore(
    context: Context,
    private val dirName: String = DIR_NAME,
    private val prefix: String = PREFIX
) {

    private val appContext = context.applicationContext

    private val imageDir: File
        get() = File(appContext.filesDir, dirName).apply { mkdirs() }

    val imagePath: String?
        get() = imageDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.startsWith(prefix) }
            ?.absolutePath

    /**
     * Copies the image behind [uri] into private storage.
     * Validates the result is a decodable, non-empty image before succeeding.
     * @return absolute path of the stored copy.
     * @throws IOException if the source cannot be read or is not a valid image.
     */
    suspend fun saveImage(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/*"
        val extension = extensionForMime(mimeType)
        val file = File(imageDir, "$prefix$extension")
        try {
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Unable to open image stream")

            if (file.length() == 0L || !isDecodableImage(file)) {
                file.delete()
                throw IOException("The selected file is not a valid image")
            }

            // Keep only the latest selection.
            imageDir.listFiles()
                ?.filter { it.isFile && it.absolutePath != file.absolutePath }
                ?.forEach { it.delete() }

            file.absolutePath
        } catch (e: Exception) {
            file.delete()
            throw IOException("Failed to store selected image", e)
        }
    }

    /** Deletes the stored image, if any. */
    suspend fun deleteImage(): Unit = withContext(Dispatchers.IO) {
        imagePath?.let { File(it).delete() }
    }

    /**
     * Reads the original display name of a content [Uri] (used for logging/UX).
     */
    suspend fun queryDisplayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    private fun extensionForMime(mimeType: String): String = when (mimeType) {
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        else -> ".jpg"
    }

    /** True if the file decodes to a real image (guards against corrupted files). */
    private fun isDecodableImage(file: File): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    private companion object {
        const val DIR_NAME = "wallpaper_images"
        const val PREFIX = "selected_image"
    }
}
