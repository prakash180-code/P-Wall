package com.prakash.pwall.utils

import android.graphics.Bitmap
import android.util.LruCache
import java.io.File

/**
 * In-memory bitmap cache shared by the preview and the wallpaper service so the
 * selected image is decoded at most once per process.
 *
 * The cache is sized from the app's memory class (see [configureForMemoryClass])
 * and shrinks on [android.content.ComponentCallbacks2.onTrimMemory] via [trim].
 *
 * Access is synchronized because it is read from multiple threads
 * (preview on main thread, wallpaper engine on its renderer thread).
 */
object BitmapCache {

    private const val BYTES_PER_MEGABYTE = 1024 * 1024

    /** Upper bound on the cache, matching the original fixed 48 MB budget. */
    private const val DEFAULT_MAX_MEGABYTES = 48

    /** Never shrink the cache below this even on low-RAM devices. */
    private const val MIN_CACHE_MEGABYTES = 16

    @Volatile
    private var maxBytes = DEFAULT_MAX_MEGABYTES * BYTES_PER_MEGABYTE

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }

        /** Public bridge to the protected [trimToSize]; evicts down to [targetBytes]. */
        fun trimTo(targetBytes: Int) = trimToSize(targetBytes)
    }

    /**
     * Sizes the cache to ~1/8 of the app's memory class, bounded to
     * [MIN_CACHE_MEGABYTES]..[DEFAULT_MAX_MEGABYTES] MB. Call once from the
     * Application with the ActivityManager memory class.
     */
    @Synchronized
    fun configureForMemoryClass(memoryClassMb: Int) {
        if (memoryClassMb <= 0) return
        val target = (memoryClassMb.toLong() * BYTES_PER_MEGABYTE / 8).coerceIn(
            MIN_CACHE_MEGABYTES.toLong() * BYTES_PER_MEGABYTE,
            DEFAULT_MAX_MEGABYTES.toLong() * BYTES_PER_MEGABYTE
        ).toInt()
        configure(target)
    }

    @Synchronized
    fun configure(maxCacheBytes: Int) {
        if (maxCacheBytes <= 0) return
        maxBytes = maxCacheBytes
        cache.resize(maxCacheBytes)
    }

    /** Drops roughly half the cached bitmaps for a low-memory signal. */
    @Synchronized
    fun trim() {
        cache.trimTo((cache.size() / 2).coerceAtLeast(0))
    }

    @Synchronized
    fun get(key: String): Bitmap? = cache.get(key)

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        if (bitmap.byteCount <= maxBytes) {
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
