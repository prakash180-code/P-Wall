package com.prakash.pwall.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.render.AnimationMath
import com.prakash.pwall.service.render.Breathing
import com.prakash.pwall.service.render.StyleRecipe
import com.prakash.pwall.service.render.WidgetPresets
import com.prakash.pwall.service.render.WidgetStyleRecipe
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
 * When the settings are a classic combination the overlay reproduces the legacy
 * combined clock/date column byte-for-byte. Any customization switches to the
 * Clock & Date Engine path where the time and date render as independent
 * widgets (own position, style, size, visibility) mirroring the wallpaper
 * engine's [ClockWidgetLayout] positions.
 *
 * Positioning matches the wallpaper engine: preset anchors align a widget to an
 * edge/corner, CUSTOM centers it on a fractional point. [extraOffset], when
 * provided, takes precedence and centers the time widget at that pixel point
 * (used for drag & drop in the customize screen).
 */
@Composable
fun ClockOverlay(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier,
    extraOffset: Offset? = null,
    dateExtraOffset: Offset? = null
) {
    val smoothTick =
        settings.smoothSecondsEnabled || settings.breathingEnabled
    val tick by produceState(initialValue = LocalDateTime.now(), settings) {
        while (true) {
            value = LocalDateTime.now()
            delay(if (smoothTick) 50L else 1000L)
        }
    }

    if (WidgetPresets.isClassicCombination(settings)) {
        ClassicClockOverlay(
            settings = settings,
            now = tick,
            modifier = modifier,
            extraOffset = extraOffset
        )
    } else {
        WidgetClockOverlay(
            settings = settings,
            now = tick,
            modifier = modifier,
            extraOffset = extraOffset,
            dateExtraOffset = dateExtraOffset
        )
    }
}

/** Byte-identical reproduction of the legacy combined clock + date column. */
@Composable
private fun ClassicClockOverlay(
    settings: WallpaperSettings,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
    extraOffset: Offset? = null
) {
    val clockText = ClockTextFormatter.formatClock(
        now, settings.timeFormat, settings.showSeconds, settings.clockLayout
    )
    val clockLines = ClockTextFormatter.formatTimeLines(
        now, settings.timeFormat, settings.showSeconds, settings.clockLayout
    )
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
 * Clock & Date Engine overlay: the time and date render as independent widgets.
 * Each widget applies its own [StyleRecipe] (font, spacing, glow, chip) so the
 * preview matches the wallpaper engine. The linked date anchors below the time
 * with the classic gap; an unlinked date floats at its own position preset.
 */
@Composable
private fun WidgetClockOverlay(
    settings: WallpaperSettings,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
    extraOffset: Offset? = null,
    dateExtraOffset: Offset? = null
) {
    val density = LocalDensity.current
    val edgePadding = with(density) { 16.dp.toPx() }
    val nowMs = System.currentTimeMillis()
    val breathScale = if (settings.breathingEnabled && settings.breathingStrength > 0f) {
        Breathing(AnimationMath.breathingPhase(nowMs), settings.breathingStrength).scale
    } else {
        1f
    }
    val breathAlpha = if (settings.breathingEnabled && settings.breathingStrength > 0f) {
        Breathing(AnimationMath.breathingPhase(nowMs), settings.breathingStrength).alpha
    } else {
        1f
    }

    val timeLines = ClockTextFormatter.formatTimeLines(
        now, settings.timeFormat, settings.showSeconds, settings.timeLayout
    )
    val timeText = timeLines.joinToString(separator = if (settings.timeLayout == TimeLayout.SPLIT) "" else "\n")
    val dateLines = ClockTextFormatter.formatDateLines(
        now, settings.dateLayout, settings.dateFormat
    )
    val dateText = dateLines.joinToString("\n")

    val timeRecipe = WidgetStyleRecipe.recipe(settings.timeStyle)
    val dateRecipe = WidgetStyleRecipe.recipe(settings.dateStyle)
    // v1.1.3: the time widget background is owned by the time container, so the
    // style-recipe chip is suppressed here to match the wallpaper engine (the
    // date widget keeps its own chip).
    val timeContainerRecipe = timeRecipe.copy(chipEnabled = false, chipAlpha = 0f)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        var timeSize by remember { mutableStateOf(IntSize.Zero) }
        var dateSize by remember { mutableStateOf(IntSize.Zero) }

        val timeTopLeft: Offset? = if (settings.clockVisible) {
            if (extraOffset != null) {
                Offset(
                    extraOffset.x - timeSize.width / 2f,
                    extraOffset.y - timeSize.height / 2f
                )
            } else {
                widgetTopLeft(
                    blockWidth = timeSize.width.toFloat(),
                    blockHeight = timeSize.height.toFloat(),
                    position = settings.position,
                    xFraction = settings.positionXFraction,
                    yFraction = settings.positionYFraction,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    edgePadding = edgePadding
                )
            }
        } else {
            null
        }

        val linkedDateTopLeft = if (settings.dateLinkedToTime && timeTopLeft != null) {
            val gap = timeSize.height * 0.18f * settings.dateGapMultiplier
            Offset(timeTopLeft.x, timeTopLeft.y + timeSize.height + gap)
        } else {
            null
        }

        if (settings.clockVisible) {
            val topLeft = timeTopLeft ?: Offset.Zero
            WidgetText(
                text = timeText,
                color = applyTransparency(settings.clockColorValue, settings.transparency),
                fontSizeSp = settings.clockFontSizeSp,
                baseFamily = settings.clockFont,
                userBold = settings.clockBold,
                userItalic = settings.clockItalic,
                recipe = timeContainerRecipe,
                userShadow = if (settings.shadowEnabled) {
                    Shadow(
                        color = settings.shadowColorValue,
                        offset = Offset(settings.shadowOffsetX, settings.shadowOffsetY),
                        blurRadius = settings.shadowBlurRadius
                    )
                } else {
                    null
                },
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = breathScale
                        scaleY = breathScale
                        alpha = breathAlpha
                    }
                    .onSizeChanged { timeSize = it }
                    .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            )
        }

        if (settings.dateVisible) {
            val topLeft = if (settings.dateLinkedToTime) {
                linkedDateTopLeft ?: Offset.Zero
            } else if (dateExtraOffset != null) {
                Offset(
                    dateExtraOffset.x - dateSize.width / 2f,
                    dateExtraOffset.y - dateSize.height / 2f
                )
            } else {
                widgetTopLeft(
                    blockWidth = dateSize.width.toFloat(),
                    blockHeight = dateSize.height.toFloat(),
                    position = settings.datePosition,
                    xFraction = settings.datePositionXFraction,
                    yFraction = settings.datePositionYFraction,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    edgePadding = edgePadding
                )
            }
            WidgetText(
                text = dateText,
                color = applyTransparency(settings.dateColorValue, settings.transparency),
                fontSizeSp = if (settings.dateFontSizeSp > 0f) {
                    settings.dateFontSizeSp
                } else {
                    settings.clockFontSizeSp * 0.36f
                },
                baseFamily = settings.dateFont,
                userBold = settings.dateBold,
                userItalic = settings.dateItalic,
                recipe = dateRecipe,
                userShadow = if (settings.dateShadowEnabled) {
                    Shadow(
                        color = settings.dateShadowColorValue,
                        offset = Offset(settings.dateShadowOffsetX, settings.dateShadowOffsetY),
                        blurRadius = settings.dateShadowBlurRadius
                    )
                } else {
                    null
                },
                modifier = Modifier
                    .onSizeChanged { dateSize = it }
                    .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
            )
        }
    }
}

/**
 * Top-left for a widget, mirroring [com.prakash.pwall.service.WallpaperRenderer.blockTopLeft]
 * (preset anchors + CUSTOM fractional centering, re-clamped on-screen).
 */
private fun widgetTopLeft(
    blockWidth: Float,
    blockHeight: Float,
    position: PositionPreset,
    xFraction: Float,
    yFraction: Float,
    maxWidth: Float,
    maxHeight: Float,
    edgePadding: Float
): Offset {
    val (x, y) = when (position) {
        PositionPreset.CENTER ->
            Pair((maxWidth - blockWidth) / 2f, (maxHeight - blockHeight) / 2f)
        PositionPreset.TOP_LEFT -> Pair(edgePadding, edgePadding)
        PositionPreset.TOP_RIGHT -> Pair(maxWidth - edgePadding - blockWidth, edgePadding)
        PositionPreset.BOTTOM_LEFT -> Pair(edgePadding, maxHeight - edgePadding - blockHeight)
        PositionPreset.BOTTOM_RIGHT ->
            Pair(maxWidth - edgePadding - blockWidth, maxHeight - edgePadding - blockHeight)
        PositionPreset.BOTTOM_CENTER ->
            Pair((maxWidth - blockWidth) / 2f, maxHeight - edgePadding - blockHeight)
        PositionPreset.CUSTOM -> clampBlockTopLeft(
            x = xFraction * maxWidth - blockWidth / 2f,
            y = yFraction * maxHeight - blockHeight / 2f,
            blockWidth = blockWidth,
            blockHeight = blockHeight,
            maxWidth = maxWidth,
            maxHeight = maxHeight
        )
    }
    return Offset(
        x.coerceIn(0f, (maxWidth - blockWidth).coerceAtLeast(0f)),
        y.coerceIn(0f, (maxHeight - blockHeight).coerceAtLeast(0f))
    )
}

/**
 * Single widget rendered as styled text: applies the [StyleRecipe] typography
 * (font, weight, letter spacing), a style glow halo, an optional frosted chip
 * and the user's own shadow beneath the crisp pass.
 */
@Composable
private fun WidgetText(
    text: String,
    color: Color,
    fontSizeSp: Float,
    baseFamily: ClockFont,
    userBold: Boolean,
    userItalic: Boolean,
    recipe: StyleRecipe,
    userShadow: Shadow?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSizeSp.sp.toPx() }
    val fontFamily = FontFamily(
        clockTypeface(
            recipe.family ?: baseFamily,
            recipe.bold ?: userBold,
            recipe.italic ?: userItalic
        )
    )
    val letterSpacing = recipe.letterSpacing.em
    val glow = if (recipe.glowRadiusFactor > 0f) {
        Shadow(
            color = Color(recipe.glowColor ?: color.toArgb().toLong()),
            offset = Offset.Zero,
            blurRadius = textSizePx * recipe.glowRadiusFactor
        )
    } else {
        null
    }

    val chipModifier = if (recipe.chipEnabled) {
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = recipe.chipAlpha.coerceIn(0f, 1f)))
    } else {
        modifier
    }

    Box(modifier = chipModifier, contentAlignment = Alignment.Center) {
        if (glow != null) {
            Text(
                text = text,
                color = color,
                fontSize = fontSizeSp.sp,
                fontFamily = fontFamily,
                letterSpacing = letterSpacing,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = glow)
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSizeSp.sp,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = userShadow)
        )
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
