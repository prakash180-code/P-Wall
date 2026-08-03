package com.prakash.pwall.service.depth

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap

/**
 * Bitmap <-> alpha-buffer conversions and compositing helpers shared by the
 * AI depth pipeline and the manual depth editor.
 *
 * The foreground mask is stored as a full-color bitmap whose alpha channel is
 * the actual mask. Editing the mask therefore reduces to rewriting that alpha
 * channel (see [withAlpha]); future tools (brush, eraser, polygon selection)
 * can stamp regions onto the same alpha buffer.
 */
object MaskMath {

    /** Extracts the alpha channel as a row-major [ByteArray] (0..255 per pixel). */
    fun alphaOf(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val alpha = ByteArray(pixels.size)
        for (i in pixels.indices) {
            alpha[i] = ((pixels[i] ushr 24) and 0xFF).toByte()
        }
        return alpha
    }

    /**
     * Returns a new bitmap identical to [source] with the alpha channel replaced
     * by [alpha] (colors are preserved, so the subject keeps its real pixels).
     */
    fun withAlpha(source: Bitmap, alpha: ByteArray): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        for (i in pixels.indices) {
            val a = alpha[i].toInt() and 0xFF
            pixels[i] = (a shl 24) or (pixels[i] and 0x00FFFFFF)
        }
        val out = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Returns [source] with a subject-shaped transparent hole where [subject] is
     * opaque. Uses DST_OUT (dest kept where the subject has no alpha), which
     * punches the hole without wiping the rest of the frame.
     */
    fun eraseSubject(source: Bitmap, subject: Bitmap): Bitmap {
        val erased = createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(erased)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.drawBitmap(
            subject,
            0f,
            0f,
            Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
        )
        return erased
    }
}
