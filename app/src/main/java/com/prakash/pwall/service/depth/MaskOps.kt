package com.prakash.pwall.service.depth

/**
 * Pure, dependency-free alpha-buffer operations used by the manual depth editor.
 *
 * Every function treats [alpha] as a per-pixel alpha channel (0..255) laid out
 * row-major, and returns a NEW buffer of the same size; the input is never
 * modified. Keeping the math free of Android types makes it unit-testable on
 * the JVM and gives future tools (brush, eraser, polygon selection) the same
 * primitives to build on.
 *
 * Border handling replicates the edge values (the same "no new content past the
 * border" behaviour as the renderer).
 */
object MaskOps {

    /** Grows the opaque region: each pixel takes the max alpha in a [radius] window. */
    fun expand(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray =
        rankFilter(alpha, width, height, radius.coerceAtLeast(0), max = true)

    /** Shrinks the opaque region: each pixel takes the min alpha in a [radius] window. */
    fun shrink(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray =
        rankFilter(alpha, width, height, radius.coerceAtLeast(0), max = false)

    /** Softens edges by box-blurring the alpha channel (semi-transparent fringe). */
    fun feather(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray =
        boxBlur(alpha, width, height, radius.coerceAtLeast(0))

    /** Removes jagged edges: blur the alpha, then re-threshold to a clean binary mask. */
    fun smooth(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        val r = radius.coerceAtLeast(0)
        val blurred = boxBlur(alpha, width, height, r)
        val out = ByteArray(blurred.size)
        for (i in out.indices) {
            out[i] = if ((blurred[i].toInt() and 0xFF) >= 128) 0xFF.toByte() else 0x00
        }
        return out
    }

    /** Separable dilation/erosion via a sliding-window rank filter. */
    private fun rankFilter(
        alpha: ByteArray,
        width: Int,
        height: Int,
        radius: Int,
        max: Boolean
    ): ByteArray {
        if (radius <= 0 || width <= 0 || height <= 0 || alpha.isEmpty()) return alpha.copyOf()
        val horizontal = rankLines(alpha, width, height, radius, max, transpose = false)
        return rankLines(horizontal, width, height, radius, max, transpose = true)
    }

    /** Applies the rank filter along rows, then along columns (index transposed). */
    private fun rankLines(
        alpha: ByteArray,
        width: Int,
        height: Int,
        radius: Int,
        max: Boolean,
        transpose: Boolean
    ): ByteArray {
        val length = if (transpose) height else width
        val count = if (transpose) width else height
        val out = ByteArray(alpha.size)
        val line = IntArray(length)
        for (c in 0 until count) {
            for (j in 0 until length) {
                val idx = if (transpose) j * width + c else c * width + j
                line[j] = alpha[idx].toInt() and 0xFF
            }
            val ranked = slidingRank(line, radius, max)
            for (j in 0 until length) {
                val idx = if (transpose) j * width + c else c * width + j
                out[idx] = ranked[j].toByte()
            }
        }
        return out
    }

    /**
     * Sliding-window min/max over a padded (edge-replicated) 1D buffer in O(n).
     * The output at `center` is the rank over `[center - radius, center + radius]`.
     */
    private fun slidingRank(input: IntArray, radius: Int, max: Boolean): IntArray {
        val n = input.size
        val pad = n + 2 * radius
        val a = IntArray(pad)
        for (i in 0 until n) a[i + radius] = input[i]
        for (i in 0 until radius) {
            a[i] = input[0]
            a[pad - 1 - i] = input[n - 1]
        }
        val out = IntArray(n)
        val deque = ArrayDeque<Int>()
        fun better(x: Int, y: Int): Boolean = if (max) x >= y else x <= y
        for (i in 0 until pad) {
            while (deque.isNotEmpty() && better(a[i], a[deque.last()])) deque.removeLast()
            deque.addLast(i)
            while (deque.isNotEmpty() && deque.first() < i - 2 * radius) deque.removeFirst()
            // The padded window [i - 2r, i] is the symmetric neighbourhood of the
            // original pixel at centre i - 2r (padded centre i - r maps back to
            // original index i - 2r).
            val center = i - 2 * radius
            if (center in 0 until n) out[center] = a[deque.first()]
        }
        return out
    }

    /** Separable box blur (prefix sums, O(n) per pass regardless of radius). */
    private fun boxBlur(alpha: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        if (radius <= 0 || width <= 0 || height <= 0 || alpha.isEmpty()) return alpha.copyOf()
        val horizontal = blurLines(alpha, width, height, radius, transpose = false)
        return blurLines(horizontal, width, height, radius, transpose = true)
    }

    private fun blurLines(
        alpha: ByteArray,
        width: Int,
        height: Int,
        radius: Int,
        transpose: Boolean
    ): ByteArray {
        val length = if (transpose) height else width
        val count = if (transpose) width else height
        val out = ByteArray(alpha.size)
        val line = IntArray(length)
        for (c in 0 until count) {
            for (j in 0 until length) {
                val idx = if (transpose) j * width + c else c * width + j
                line[j] = alpha[idx].toInt() and 0xFF
            }
            val blurred = blurLine(line, radius)
            for (j in 0 until length) {
                val idx = if (transpose) j * width + c else c * width + j
                out[idx] = blurred[j].toByte()
            }
        }
        return out
    }

    /** Clamped box average: each output is the mean of the window centred on it. */
    private fun blurLine(input: IntArray, radius: Int): IntArray {
        val n = input.size
        val prefix = LongArray(n + 1)
        for (i in 0 until n) prefix[i + 1] = prefix[i] + input[i]
        val out = IntArray(n)
        for (i in 0 until n) {
            val lo = (i - radius).coerceAtLeast(0)
            val hi = (i + radius).coerceAtMost(n - 1)
            val sum = prefix[hi + 1] - prefix[lo]
            out[i] = (sum / (hi - lo + 1)).toInt()
        }
        return out
    }
}
