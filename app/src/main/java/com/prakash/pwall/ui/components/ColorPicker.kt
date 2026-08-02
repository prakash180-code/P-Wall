package com.prakash.pwall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PresetColors = listOf(
    Color.White,
    Color(0xFFBDBDBD),
    Color(0xFF757575),
    Color.Black,
    Color(0xFFF44336),
    Color(0xFFFF5722),
    Color(0xFFFF9800),
    Color(0xFFFFC107),
    Color(0xFFFFEB3B),
    Color(0xFFCDDC39),
    Color(0xFF4CAF50),
    Color(0xFF009688),
    Color(0xFF00BCD4),
    Color(0xFF03A9F4),
    Color(0xFF2196F3),
    Color(0xFF3F51B5),
    Color(0xFF673AB7),
    Color(0xFF9C27B0),
    Color(0xFFE91E63),
    Color(0xFF795548)
)

/**
 * Lightweight color picker: preset swatches plus HSV sliders. No external
 * dependencies.
 */
@Composable
fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hsv by remember(color) {
        mutableStateOf(toHsvArray(color))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PresetColors.chunked(PresetColors.size / 2).forEach { rowColors ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowColors.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .then(
                                    if (preset == color) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onColorChange(preset) }
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            ColorSlider("Hue", hsv[0] / 360f) {
                hsv = floatArrayOf(it * 360f, hsv[1], hsv[2])
                onColorChange(hsvColor(hsv))
            }
            ColorSlider("Saturation", hsv[1]) {
                hsv = floatArrayOf(hsv[0], it, hsv[2])
                onColorChange(hsvColor(hsv))
            }
            ColorSlider("Brightness", hsv[2]) {
                hsv = floatArrayOf(hsv[0], hsv[1], it)
                onColorChange(hsvColor(hsv))
            }
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 96.dp, height = 24.dp)
        )
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun toHsvArray(color: Color): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), out)
    return out
}

private fun hsvColor(hsv: FloatArray): Color =
    Color(android.graphics.Color.HSVToColor(hsv))
