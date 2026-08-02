package com.prakash.pwall.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.applyTransparency
import com.prakash.pwall.utils.clockTypeface
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Live clock + date overlay driven by [settings]. Recomputes text every second.
 *
 * Positioning matches the wallpaper engine: preset anchors align the text block
 * to an edge/corner, CUSTOM centers it on a fractional point. [extraOffset], when
 * provided, takes precedence and centers the block at that pixel point (used for
 * drag & drop in the customize screen).
 */
@Composable
fun ClockOverlay(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier,
    extraOffset: Offset? = null
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

    val density = LocalDensity.current
    val edgePadding = with(density) { 16.dp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        var columnSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .onSizeChanged { columnSize = it }
                    .offset {
                        val w = columnSize.width.toFloat()
                        val h = columnSize.height.toFloat()
                        when {
                            extraOffset != null -> IntOffset(
                                (extraOffset.x - w / 2f).roundToInt(),
                                (extraOffset.y - h / 2f).roundToInt()
                            )

                            settings.position == PositionPreset.CENTER -> IntOffset(
                                ((maxWidth - w) / 2f).roundToInt(),
                                ((maxHeight - h) / 2f).roundToInt()
                            )

                            settings.position == PositionPreset.TOP_LEFT -> IntOffset(
                                edgePadding.roundToInt(),
                                edgePadding.roundToInt()
                            )

                            settings.position == PositionPreset.TOP_RIGHT -> IntOffset(
                                (maxWidth - edgePadding - w).roundToInt(),
                                edgePadding.roundToInt()
                            )

                            settings.position == PositionPreset.BOTTOM_LEFT -> IntOffset(
                                edgePadding.roundToInt(),
                                (maxHeight - edgePadding - h).roundToInt()
                            )

                            settings.position == PositionPreset.BOTTOM_RIGHT -> IntOffset(
                                (maxWidth - edgePadding - w).roundToInt(),
                                (maxHeight - edgePadding - h).roundToInt()
                            )

                            settings.position == PositionPreset.BOTTOM_CENTER -> IntOffset(
                                ((maxWidth - w) / 2f).roundToInt(),
                                (maxHeight - edgePadding - h).roundToInt()
                            )

                            else -> { // CUSTOM
                                IntOffset(
                                    (settings.positionXFraction * maxWidth - w / 2f).roundToInt(),
                                    (settings.positionYFraction * maxHeight - h / 2f).roundToInt()
                                )
                            }
                        }
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
