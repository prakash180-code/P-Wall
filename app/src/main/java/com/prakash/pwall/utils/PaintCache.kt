package com.prakash.pwall.utils

import android.graphics.Paint

/**
 * Reuses [Paint] instances across render frames. The wallpaper draws up to a
 * dozen paints per frame (time, date, glow passes); building fresh ones every
 * frame allocates unnecessarily. Entries are keyed by an exact configuration
 * signature (see [com.prakash.pwall.service.render.PaintKey]) so a settings
 * change produces a new key and therefore a fresh paint instead of a stale one.
 *
 * The cache is bounded by [maxEntries]; once it overflows it is cleared
 * wholesale, which is safe because settings change only occasionally.
 *
 * Paints are mutated for per-frame alpha (see [com.prakash.pwall.service.render.ClockDraw]),
 * so each rendering engine keeps its own cache to avoid cross-thread races.
 */
class PaintCache(private val maxEntries: Int = 16) {

    private val cache = HashMap<String, Paint>()
    private val accessOrder = ArrayList<String>()

    /** Returns the cached paint for [key] or builds and stores a new one. */
    @Synchronized
    fun get(key: String, factory: () -> Paint): Paint {
        cache[key]?.let { return it }
        val paint = factory()
        cache[key] = paint
        accessOrder.add(key)
        while (accessOrder.size > maxEntries) {
            cache.remove(accessOrder.removeAt(0))
        }
        return paint
    }

    @Synchronized
    fun clear() {
        cache.clear()
        accessOrder.clear()
    }
}

/** Fallback cache for callers that do not own an engine (legacy facades, tests). */
internal val SharedPaintCache: PaintCache = PaintCache()
