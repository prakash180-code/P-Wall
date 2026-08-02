package com.prakash.pwall.utils

import androidx.compose.ui.graphics.Color

/** Blends a [color] with the wallpaper transparency (0..100). */
fun applyTransparency(color: Color, transparencyPercent: Int): Color {
    val clamped = transparencyPercent.coerceIn(0, 100)
    return color.copy(alpha = color.alpha * clamped / 100f)
}
