package com.prakash.pwall.service.render

import android.graphics.Bitmap
import com.prakash.pwall.data.model.WallpaperSettings
import java.time.LocalDateTime

/**
 * Immutable snapshot of everything a single render pass needs. Layers are
 * stateless; each frame carries the full context so every layer produces the
 * same output the legacy renderer did. The clock block is resolved lazily once
 * per frame and shared by the clock/date layers (single paint allocation, no
 * duplicated layout math).
 */
data class RenderFrame(
    val settings: WallpaperSettings,
    val backgroundBitmap: Bitmap? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    val displayDensity: Float = 1f,
    val width: Int = 0,
    val height: Int = 0
) {
    val clockBlock: ClockBlockLayout.Block by lazy {
        ClockBlockLayout.resolve(
            canvasWidth = width.toFloat(),
            canvasHeight = height.toFloat(),
            settings = settings,
            displayDensity = displayDensity,
            now = now
        )
    }
}
