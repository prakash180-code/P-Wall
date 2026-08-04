package com.prakash.pwall.service.render

import android.graphics.Bitmap
import android.graphics.Matrix
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.color.ColorPalette
import com.prakash.pwall.service.motion.MotionFrame
import com.prakash.pwall.service.motion.ParallaxMath
import com.prakash.pwall.utils.PaintCache
import java.time.LocalDateTime
import kotlin.math.min

/**
 * Immutable snapshot of everything a single render pass needs. Layers are
 * stateless; each frame carries the full context so every layer produces the
 * same output the legacy renderer did. The clock block is resolved lazily once
 * per frame and shared by the clock/date layers (single paint allocation, no
 * duplicated layout math). [motion] carries the current 3D parallax tilt, if any.
 *
 * Premium frame data: [palette] feeds the dynamic colors, [timeTransition] and
 * [breathing] drive the micro-animations, and [cinematicZoom] applies the
 * slow Ken Burns sweep to the shared background matrix.
 */
data class RenderFrame(
    val settings: WallpaperSettings,
    val backgroundBitmap: Bitmap? = null,
    val foregroundBitmap: Bitmap? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    val displayDensity: Float = 1f,
    val width: Int = 0,
    val height: Int = 0,
    val motion: MotionFrame? = null,
    val palette: ColorPalette? = null,
    val timeTransition: TimeTransition? = null,
    val breathing: Breathing? = null,
    val cinematicZoom: Float = 1f,
    val paintCache: PaintCache? = null
) {
    val clockBlock: ClockBlockLayout.Block by lazy {
        ClockBlockLayout.resolve(
            canvasWidth = width.toFloat(),
            canvasHeight = height.toFloat(),
            settings = settings,
            displayDensity = displayDensity,
            now = now,
            motion = motion,
            palette = palette,
            paintCache = paintCache
        )
    }

    /**
     * The single [Matrix] that maps the source image onto the canvas, including
     * the 3D parallax shift and the cinematic zoom. Shared by the background
     * layer and the AI-depth foreground layer so the extracted subject always
     * stays pixel-aligned with the image beneath it (even while the device is
     * tilted or the camera slowly zooms).
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
            if (settings.debugParallax) {
                // Debug mode: amplify the shift to ~±50 px for visual
                // verification, backed by a small base zoom so edges never show.
                base.postScale(
                    ParallaxMath.DEBUG_BASE_ZOOM,
                    ParallaxMath.DEBUG_BASE_ZOOM,
                    width / 2f,
                    height / 2f
                )
                val (shiftX, shiftY) = ParallaxMath.debugShiftPx(
                    tiltX = m.tiltX,
                    tiltY = m.tiltY,
                    sensitivity = settings.parallaxSensitivityValue,
                    strength = settings.parallaxStrength,
                    minScreenDim = min(width, height).toFloat()
                )
                base.postTranslate(shiftX, shiftY)
            } else {
                val (maxPanX, maxPanY) = WallpaperRenderer.backgroundPanBounds(
                    bitmap.width, bitmap.height, width, height, settings
                )
                val userPanX = settings.backgroundTranslateXFraction.coerceIn(-1f, 1f) * maxPanX
                val userPanY = settings.backgroundTranslateYFraction.coerceIn(-1f, 1f) * maxPanY
                val (shiftX, shiftY) = ParallaxMath.backgroundShiftPx(
                    tiltX = m.tiltX,
                    tiltY = m.tiltY,
                    sensitivity = settings.parallaxSensitivityValue,
                    strength = settings.parallaxStrength,
                    minScreenDim = min(width, height).toFloat(),
                    maxPanX = maxPanX,
                    maxPanY = maxPanY,
                    userPanX = userPanX,
                    userPanY = userPanY
                )
                base.postTranslate(shiftX, shiftY)
            }
        }
        if (cinematicZoom != 1f && width > 0 && height > 0) {
            base.postScale(cinematicZoom, cinematicZoom, width / 2f, height / 2f)
        }
        base
    }
}
