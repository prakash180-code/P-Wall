package com.prakash.pwall.service.render

import android.graphics.Bitmap
import com.prakash.pwall.data.model.WallpaperSettings
import java.time.LocalDateTime

/**
 * Immutable snapshot of everything a single render pass needs. Layers are
 * stateless; each frame carries the full context so every layer produces the
 * same output the legacy renderer did.
 */
data class RenderFrame(
    val settings: WallpaperSettings,
    val backgroundBitmap: Bitmap? = null,
    val now: LocalDateTime = LocalDateTime.now(),
    val displayDensity: Float = 1f
)
