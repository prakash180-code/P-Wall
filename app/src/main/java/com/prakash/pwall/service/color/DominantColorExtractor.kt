package com.prakash.pwall.service.color

import android.annotation.SuppressLint
import android.graphics.Bitmap

/**
 * Extracts a small [ColorPalette] of dominant colors from a wallpaper image.
 *
 * The core quantization is pure (operates on ARGB pixel ints) so it can be unit
 * tested on the JVM; [extract] adds the Android bitmap sampling on top. Colors
 * are quantized into a coarse RGB histogram, then merged by distance so similar
 * shades collapse into one dominant entry.
 */
object DominantColorExtractor {

    /** Histogram bucket bits per channel (5 bits -> 32 levels per channel). */
    private const val QUANT_BITS = 5

    private const val QUANT_MASK = (1 shl QUANT_BITS) - 1

    /** Two colors closer than this are merged into one. */
    private const val MERGE_DISTANCE = 46f

    /** Merge candidates are kept from the top of the frequency ranking. */
    private const val CANDIDATE_POOL = 12

    /**
     * Extracts up to [maxColors] dominant colors from a [Bitmap] using a small
     * downsampled sample so the cost stays constant regardless of image size.
     */
    fun extract(bitmap: Bitmap, maxColors: Int = 4): ColorPalette {
        val sample = downsample(bitmap, maxSampleDimension = 96) ?: return ColorPalette(emptyList())
        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        if (sample !== bitmap) sample.recycle()
        return ColorPalette(quantize(pixels.toList(), maxColors))
    }

    /**
     * Pure quantization over ARGB pixels. Ignores transparent pixels, builds a
     * weighted coarse-RGB histogram, ranks buckets by count and merges nearby
     * colors into up to [maxColors] dominant entries ordered by frequency.
     */
    fun quantize(pixels: List<Int>, maxColors: Int): List<Int> {
        if (pixels.isEmpty() || maxColors <= 0) return emptyList()

        val histogram = HashMap<Int, Long>()
        for (p in pixels) {
            val alpha = (p ushr 24) and 0xFF
            if (alpha < 64) continue
            val key = quantizedKey(p)
            histogram[key] = (histogram[key] ?: 0L) + 1L
        }
        if (histogram.isEmpty()) return listOf(ColorPalette.WHITE)

        val ranked = histogram.entries.sortedByDescending { it.value }
        val weightSum = ranked.sumOf { it.value }
        val threshold = maxOf(1L, weightSum / (maxColors * 20L))

        val buckets = ranked
            .filter { it.value >= threshold }
            .take(CANDIDATE_POOL)
            .map { Bucket(key = it.key, count = it.value) }

        if (buckets.isEmpty()) {
            return ranked.take(maxColors).map { averageColor(it.key) }
        }

        // Merge nearest buckets greedily until at most maxColors remain.
        val merged = mutableListOf(buckets.first())
        for (i in 1 until buckets.size) {
            val candidate = buckets[i]
            val nearest = merged
                .map { it to colorDistance(it.averageColor(), candidate.averageColor()) }
                .minByOrNull { it.second }
            if (nearest != null && nearest.second < MERGE_DISTANCE) {
                val (target, _) = nearest
                target.absorb(candidate)
            } else {
                merged.add(candidate)
            }
            if (merged.size >= maxColors * 2) break
        }

        return merged
            .sortedByDescending { it.count }
            .take(maxColors)
            .map { it.averageColor() }
            .sortedByDescending { histogram[averageKey(it)] ?: 0L }
    }

    private class Bucket(val key: Int, var count: Long) {
        private val step = 1 shl (8 - QUANT_BITS)
        private var sumR = 0L
        private var sumG = 0L
        private var sumB = 0L

        init {
            sumR = expand((key ushr 16) and QUANT_MASK) * count
            sumG = expand((key ushr 8) and QUANT_MASK) * count
            sumB = expand(key and QUANT_MASK) * count
        }

        private fun expand(v: Int): Long = ((v * step) + step / 2).coerceIn(0, 255).toLong()

        fun absorb(other: Bucket) {
            val total = count + other.count
            val newR = (sumR * count + other.sumR * other.count) / total
            val newG = (sumG * count + other.sumG * other.count) / total
            val newB = (sumB * count + other.sumB * other.count) / total
            sumR = newR
            sumG = newG
            sumB = newB
            count = total
        }

        fun averageColor(): Int =
            (0xFF shl 24) or
                ((sumR / count).toInt().coerceIn(0, 255) shl 16) or
                ((sumG / count).toInt().coerceIn(0, 255) shl 8) or
                (sumB / count).toInt().coerceIn(0, 255)
    }

    private fun quantizedKey(argb: Int): Int {
        val r = ((argb ushr 16) and 0xFF) shr (8 - QUANT_BITS)
        val g = ((argb ushr 8) and 0xFF) shr (8 - QUANT_BITS)
        val b = (argb and 0xFF) shr (8 - QUANT_BITS)
        return (r and QUANT_MASK) shl 16 or ((g and QUANT_MASK) shl 8) or (b and QUANT_MASK)
    }

    private fun averageKey(argb: Int): Int {
        val r = ((argb ushr 16) and 0xFF) shr (8 - QUANT_BITS)
        val g = ((argb ushr 8) and 0xFF) shr (8 - QUANT_BITS)
        val b = (argb and 0xFF) shr (8 - QUANT_BITS)
        return (r and QUANT_MASK) shl 16 or ((g and QUANT_MASK) shl 8) or (b and QUANT_MASK)
    }

    private fun averageColor(key: Int): Int {
        val step = 1 shl (8 - QUANT_BITS)
        val r = (((key ushr 16) and QUANT_MASK) * step + step / 2).coerceIn(0, 255)
        val g = (((key ushr 8) and QUANT_MASK) * step + step / 2).coerceIn(0, 255)
        val b = ((key and QUANT_MASK) * step + step / 2).coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Squared RGB distance between two colors. */
    private fun colorDistance(a: Int, b: Int): Float {
        val dr = ((a ushr 16) and 0xFF) - ((b ushr 16) and 0xFF)
        val dg = ((a ushr 8) and 0xFF) - ((b ushr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return (dr * dr + dg * dg + db * db).toFloat()
    }

    @SuppressLint("UseKtx")
    private fun downsample(bitmap: Bitmap, maxSampleDimension: Int): Bitmap? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSampleDimension) return bitmap
        val scale = maxSampleDimension.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
