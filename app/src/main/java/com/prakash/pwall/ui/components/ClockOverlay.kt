package com.prakash.pwall.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.render.AnimationMath
import com.prakash.pwall.service.render.Breathing
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.applyTransparency
import com.prakash.pwall.utils.clockTypeface
import com.prakash.pwall.utils.clampBlockTopLeft
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Live clock + date overlay driven by [settings]. Recomputed continuously so the
 * premium micro-animations work: a smooth-sweeping second hand, a 420 ms
 * cross-fade whenever the digits change, a gentle breathing pulse, and an
 * optional glass glow behind the text.
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
    val smoothTick =
        settings.smoothSecondsEnabled || settings.breathingEnabled
    val tick by produceState(initialValue = LocalDateTime.now(), settings) {
        while (true) {
            value = LocalDateTime.now()
            delay(if (smoothTick) 50L else 1000L)
        }
    }

    val clockText = ClockTextFormatter.formatClock(
        tick, settings.timeFormat, settings.showSeconds, settings.clockLayout
    )
    val clockLines = ClockTextFormatter.formatTimeLines(
        tick, settings.timeFormat, settings.showSeconds, settings.clockLayout
    )
    val dateText = ClockTextFormatter.formatDate(tick, settings.dateFormat)

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
    val glow = if (settings.glassEnabled && settings.glassGlowRadius > 0f) {
        Shadow(
            color = settings.glassGlowColorValue,
            offset = Offset.Zero,
            blurRadius = settings.glassGlowRadius
        )
    } else {
        null
    }

    val nowMs = System.currentTimeMillis()
    val breathing = if (settings.breathingEnabled && settings.breathingStrength > 0f) {
        Breathing(AnimationMath.breathingPhase(nowMs), settings.breathingStrength)
    } else {
        null
    }
    val breathScale = breathing?.scale ?: 1f
    val breathAlpha = breathing?.alpha ?: 1f

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
            AnimatedContent(
                targetState = clockText to dateText,
                transitionSpec = {
                    if (settings.fadeTransitionsEnabled) {
                        (fadeIn(animationSpec = tween(AnimationMath.TRANSITION_MS.toInt()))
                            togetherWith
                            fadeOut(animationSpec = tween(AnimationMath.TRANSITION_MS.toInt())))
                    } else {
                        (fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0)))
                    }
                },
                label = "clockTransition",
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = breathScale
                        scaleY = breathScale
                        alpha = breathAlpha
                    }
                    .offset {
                        val w = columnSize.width.toFloat()
                        val h = columnSize.height.toFloat()
                        val topLeft: Pair<Float, Float> = when {
                            extraOffset != null -> clampBlockTopLeft(
                                x = extraOffset.x - w / 2f,
                                y = extraOffset.y - h / 2f,
                                blockWidth = w,
                                blockHeight = h,
                                maxWidth = maxWidth,
                                maxHeight = maxHeight
                            )

                            settings.position == PositionPreset.CENTER ->
                                Pair((maxWidth - w) / 2f, (maxHeight - h) / 2f)

                            settings.position == PositionPreset.TOP_LEFT ->
                                Pair(edgePadding, edgePadding)

                            settings.position == PositionPreset.TOP_RIGHT ->
                                Pair(maxWidth - edgePadding - w, edgePadding)

                            settings.position == PositionPreset.BOTTOM_LEFT ->
                                Pair(edgePadding, maxHeight - edgePadding - h)

                            settings.position == PositionPreset.BOTTOM_RIGHT ->
                                Pair(maxWidth - edgePadding - w, maxHeight - edgePadding - h)

                            settings.position == PositionPreset.BOTTOM_CENTER ->
                                Pair((maxWidth - w) / 2f, maxHeight - edgePadding - h)

                            else -> { // CUSTOM
                                clampBlockTopLeft(
                                    x = settings.positionXFraction * maxWidth - w / 2f,
                                    y = settings.positionYFraction * maxHeight - h / 2f,
                                    blockWidth = w,
                                    blockHeight = h,
                                    maxWidth = maxWidth,
                                    maxHeight = maxHeight
                                )
                            }
                        }
                        IntOffset(topLeft.first.roundToInt(), topLeft.second.roundToInt())
                    }
            ) { (clock, date) ->
                ClockColumn(
                    timeLines = clockLines,
                    dateText = date,
                    clockColor = clockColor,
                    dateColor = dateColor,
                    clockFontSize = settings.clockFontSizeSp.sp,
                    fontFamily = fontFamily,
                    shadow = shadow,
                    glow = glow,
                    modifier = Modifier.onSizeChanged { columnSize = it }
                )
            }
        }
    }
}

/**
 * Renders the stacked time lines + date; an extra pass adds the glass glow
 * behind each line. The horizontal layout is a single line; the vertical
 * layouts stack the (2-digit, aligned) digits and optional markers.
 */
@Composable
private fun ClockColumn(
    timeLines: List<String>,
    dateText: String,
    clockColor: Color,
    dateColor: Color,
    clockFontSize: TextUnit,
    fontFamily: FontFamily,
    shadow: Shadow?,
    glow: Shadow?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        timeLines.forEach { line ->
            TextLine(
                text = line,
                color = clockColor,
                fontSize = clockFontSize,
                fontFamily = fontFamily,
                shadow = shadow,
                glow = glow
            )
        }
        TextLine(
            text = dateText,
            color = dateColor,
            fontSize = clockFontSize * 0.36f,
            fontFamily = fontFamily,
            shadow = shadow,
            glow = glow
        )
    }
}

@Composable
private fun TextLine(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    shadow: Shadow?,
    glow: Shadow?
) {
    Box(contentAlignment = Alignment.TopCenter) {
        if (glow != null) {
            Text(
                text = text,
                color = color,
                fontSize = fontSize,
                fontFamily = fontFamily,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = glow)
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = shadow)
        )
    }
}

/** Shared placeholder color used when no image is selected yet. */
val PreviewPlaceholderColor: Color = Color(0xFF1C1C2A)
