package com.prakash.pwall.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.prakash.pwall.data.model.ClockFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes and caches the wallpaper bitmap. Shared by preview and service. */
object ImageLoader {

    /** Cap on the longest edge; keeps memory usage predictable. */
    const val MAX_DIMENSION = 1920

    /** Returns the cached bitmap for [path], if any. */
    fun cached(path: String): Bitmap? = BitmapCache.get(BitmapCache.keyFor(path))

    /** Loads (or reuses from cache) the sampled bitmap for [path]. */
    suspend fun load(path: String?): ImageBitmap? {
        if (path == null) return null
        cached(path)?.let { return it.asImageBitmap() }
        return withContext(Dispatchers.IO) {
            val bitmap = decodeSampled(path) ?: return@withContext null
            BitmapCache.put(BitmapCache.keyFor(path), bitmap)
            bitmap.asImageBitmap()
        }
    }

    /** Decodes [path] downscaled so the longest edge is at most [MAX_DIMENSION]. */
    fun decodeSampled(path: String): Bitmap? =
        decodeSampled(path, MAX_DIMENSION)

    /**
     * Decodes [path] downscaled so the longest edge is at most [maxDimension].
     * Low-end devices pass [com.prakash.pwall.service.performance.LowEndDevice.LOW_END_MAX_DIMENSION]
     * to keep the decoded wallpaper within a smaller memory budget.
     */
    fun decodeSampled(path: String, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxDimension ||
            bounds.outHeight / (sampleSize * 2) >= maxDimension
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return runCatching { BitmapFactory.decodeFile(path, options) }.getOrNull()
    }
}

/** Builds the [Typeface] used for the clock, honoring bold/italic flags. */
fun clockTypeface(font: ClockFont, bold: Boolean, italic: Boolean): Typeface {
    val style = when {
        bold && italic -> Typeface.BOLD_ITALIC
        bold -> Typeface.BOLD
        italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    return Typeface.create(font.familyName, style)
}
