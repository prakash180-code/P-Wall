package com.prakash.pwall.utils

import android.graphics.Bitmap
import android.util.LruCache
import java.io.File

/**
 * Simple in-memory bitmap cache shared by the preview and the wallpaper
 * service so the selected image is decoded at most once per process.
 *
 * Access is synchronized because it is read from multiple threads
 * (preview on main thread, wallpaper engine on its renderer thread).
 */
object BitmapCache {

    private const val MAX_CACHE_MEGABYTES = 48
    private const val BYTES_PER_MEGABYTE = 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(
        MAX_CACHE_MEGABYTES * BYTES_PER_MEGABYTE
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    @Synchronized
    fun get(key: String): Bitmap? = cache.get(key)

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        if (bitmap.byteCount <= MAX_CACHE_MEGABYTES * BYTES_PER_MEGABYTE) {
            cache.put(key, bitmap)
        }
    }

    @Synchronized
    fun remove(key: String) {
        cache.remove(key)
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    fun keyFor(path: String): String = File(path).name
}
