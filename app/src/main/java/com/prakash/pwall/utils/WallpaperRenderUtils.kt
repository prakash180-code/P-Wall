package com.prakash.pwall.utils

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.prakash.pwall.data.model.PositionPreset

/** Maps a position preset to a Compose [Alignment] (excluding CUSTOM). */
fun positionPresetToAlignment(preset: PositionPreset): Alignment {
    return when (preset) {
        PositionPreset.CENTER -> Alignment.Center
        PositionPreset.TOP_LEFT -> Alignment.TopStart
        PositionPreset.TOP_RIGHT -> Alignment.TopEnd
        PositionPreset.BOTTOM_LEFT -> Alignment.BottomStart
        PositionPreset.BOTTOM_RIGHT -> Alignment.BottomEnd
        PositionPreset.BOTTOM_CENTER -> Alignment.BottomCenter
        PositionPreset.CUSTOM -> Alignment.Center
    }
}

/** Blends a [color] with the wallpaper transparency (0..100). */
fun applyTransparency(color: Color, transparencyPercent: Int): Color {
    val clamped = transparencyPercent.coerceIn(0, 100)
    return color.copy(alpha = color.alpha * clamped / 100f)
}
