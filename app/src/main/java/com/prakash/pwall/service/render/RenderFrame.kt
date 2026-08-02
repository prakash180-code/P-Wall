package com.prakash.pwall.service.render

import android.graphics.Bitmap
import android.graphics.Matrix
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.motion.MotionFrame
import com.prakash.pwall.service.motion.ParallaxMath
import java.time.LocalDateTime
import kotlin.math.min

/**
 * Immutable snapshot of everything a single render pass needs. Layers are
 * stateless; each frame carries the full context so every layer produces the
 * same output the legacy renderer did. The clock block is resolved lazily once
 * per frame and shared by the clock/date layers (single paint allocation, no
 * duplicated layout math). [motion] carries the current 3D parallax tilt, if any.
 */
data class RenderFrame(
    val settings: WallpaperSettings,
    val backgroundBitmap: Bitmap? = null,
    val foregroundBitmap: Bitmap? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    val displayDensity: Float = 1f,
    val width: Int = 0,
    val height: Int = 0,
    val motion: MotionFrame? = null
) {
    val clockBlock: ClockBlockLayout.Block by lazy {
        ClockBlockLayout.resolve(
            canvasWidth = width.toFloat(),
            canvasHeight = height.toFloat(),
            settings = settings,
            displayDensity = displayDensity,
            now = now,
            motion = motion
        )
    }

    /**
     * The single [Matrix] that maps the source image onto the canvas, including
     * the 3D parallax shift. Shared by the background layer and the AI-depth
     * foreground layer so the extracted subject always stays pixel-aligned with
     * the image beneath it (even while the device is tilted).
     */
    val backgroundMatrix: Matrix by lazy {
        val bitmap = backgroundBitmap
        val base = if (bitmap == null) {
            Matrix()
        } else {
            WallpaperRenderer.backgroundMatrix(
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                targetW = width,
                targetH = height,
                mode = settings.backgroundMode,
                zoom = settings.backgroundZoom,
                rotationDegrees = settings.backgroundRotationDegrees,
                translateXFraction = settings.backgroundTranslateXFraction,
                translateYFraction = settings.backgroundTranslateYFraction
            )
        }
        val m = motion
        if (m != null && bitmap != null) {
            val (maxPanX, maxPanY) = WallpaperRenderer.backgroundPanBounds(
                bitmap.width, bitmap.height, width, height, settings
            )
            val userPanX = settings.backgroundTranslateXFraction.coerceIn(-1f, 1f) * maxPanX
            val userPanY = settings.backgroundTranslateYFraction.coerceIn(-1f, 1f) * maxPanY
            val (shiftX, shiftY) = ParallaxMath.backgroundShiftPx(
                tiltX = m.tiltX,
                tiltY = m.tiltY,
                sensitivity = settings.parallaxSensitivity,
                strength = settings.parallaxStrength,
                minScreenDim = min(width, height).toFloat(),
                maxPanX = maxPanX,
                maxPanY = maxPanY,
                userPanX = userPanX,
                userPanY = userPanY
            )
            base.postTranslate(shiftX, shiftY)
        }
        base
    }
}
