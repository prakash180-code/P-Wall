package com.prakash.pwall.data.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Stores the user's selected wallpaper image in app-private storage so it can
 * be read later by the live wallpaper service without extra permissions.
 *
 * Images are copied from any [Uri] (photo picker or Storage Access Framework).
 */
class ImageStore(context: Context) {

    private val appContext = context.applicationContext

    private val imageDir: File
        get() = File(appContext.filesDir, DIR_NAME).apply { mkdirs() }

    val imagePath: String?
        get() = imageDir.listFiles()
            ?.firstOrNull { it.isFile && it.name.startsWith(PREFIX) }
            ?.absolutePath

    /**
     * Copies the image behind [uri] into private storage.
     * @return absolute path of the stored copy.
     */
    suspend fun saveImage(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val resolver = appContext.contentResolver
            val mimeType = resolver.getType(uri) ?: "image/*"
            val extension = extensionForMime(mimeType)
            val file = File(imageDir, "$PREFIX$extension")
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Unable to open image stream")

            // Keep only the latest selection.
            imageDir.listFiles()
                ?.filter { it.isFile && it.absolutePath != file.absolutePath }
                ?.forEach { it.delete() }

            file.absolutePath
        } catch (e: Exception) {
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

    private companion object {
        const val DIR_NAME = "wallpaper_images"
        const val PREFIX = "selected_image"
    }
}
