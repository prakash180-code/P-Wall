package com.prakash.pwall.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.applyTransparency
import com.prakash.pwall.utils.clockTypeface
import com.prakash.pwall.utils.positionPresetToAlignment
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Live clock + date overlay driven by [settings]. Recomputes text every second
 * and is used by both the preview and (in adapted form) the wallpaper engine.
 */
@Composable
fun ClockOverlay(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier
) {
    val now by produceState(initialValue = LocalDateTime.now(), settings.showSeconds) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000L)
        }
    }

    val clockText = ClockTextFormatter.formatTime(now, settings.timeFormat, settings.showSeconds)
    val dateText = ClockTextFormatter.formatDate(now, settings.dateFormat)

    val clockColor = applyTransparency(settings.clockColorValue, settings.transparency)
    val dateColor = applyTransparency(settings.dateColorValue, settings.transparency)
    val fontFamily = FontFamily(
        clockTypeface(settings.clockFont, settings.clockBold, settings.clockItalic)
    )
    val shadow = if (settings.shadowEnabled) {
        Shadow(
            color = settings.shadowColorValue,
            offset = Offset(settings.shadowOffsetX, settings.shadowOffsetY),
            blurRadius = settings.shadowBlurRadius
        )
    } else {
        null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val anchor = positionPresetToAlignment(settings.position)
        val isCustom = settings.position == PositionPreset.CUSTOM
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        val offset = if (isCustom) {
            Offset(
                x = (settings.positionXFraction - 0.5f) * maxWidth * 0.9f,
                y = (settings.positionYFraction - 0.5f) * maxHeight * 0.9f
            )
        } else {
            Offset.Zero
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = anchor
        ) {
            Column(
                modifier = Modifier.offset {
                    IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = clockText,
                    color = clockColor,
                    fontSize = settings.clockFontSizeSp.sp,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = shadow)
                )
                Text(
                    text = dateText,
                    color = dateColor,
                    fontSize = (settings.clockFontSizeSp * 0.36f).sp,
                    fontFamily = fontFamily,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = shadow)
                )
            }
        }
    }
}

/** Shared placeholder color used when no image is selected yet. */
val PreviewPlaceholderColor: Color = Color(0xFF1C1C2A)
