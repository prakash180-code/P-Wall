package com.prakash.pwall.utils

import androidx.compose.ui.graphics.Color

/** Blends a [color] with the wallpaper transparency (0..100). */
fun applyTransparency(color: Color, transparencyPercent: Int): Color {
    val clamped = transparencyPercent.coerceIn(0, 100)
    return color.copy(alpha = color.alpha * clamped / 100f)
}

/**
 * Clamps a block's top-left (x, y) of size [blockWidth] x [blockHeight] so the
 * whole block stays inside a 0..[maxWidth] x 0..[maxHeight] area. Shared by the
 * Compose preview and the wallpaper engine so drag positioning never pushes the
 * clock off-screen. When the block is larger than the area it is centered.
 */
fun clampBlockTopLeft(
    x: Float,
    y: Float,
    blockWidth: Float,
    blockHeight: Float,
    maxWidth: Float,
    maxHeight: Float
): Pair<Float, Float> {
    val centerX = x + blockWidth / 2f
    val centerY = y + blockHeight / 2f
    val clampedX = if (blockWidth < maxWidth) {
        centerX.coerceIn(blockWidth / 2f, maxWidth - blockWidth / 2f) - blockWidth / 2f
    } else {
        (maxWidth - blockWidth) / 2f
    }
    val clampedY = if (blockHeight < maxHeight) {
        centerY.coerceIn(blockHeight / 2f, maxHeight - blockHeight / 2f) - blockHeight / 2f
    } else {
        (maxHeight - blockHeight) / 2f
    }
    return clampedX to clampedY
}
