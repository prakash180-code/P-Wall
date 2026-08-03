package com.prakash.pwall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.color.ColorPalette
import com.prakash.pwall.service.color.DominantColorExtractor
import com.prakash.pwall.service.effects.CinematicZoom
import com.prakash.pwall.service.render.AnimationMath
import com.prakash.pwall.service.render.Breathing
import com.prakash.pwall.service.render.PremiumEffectsModule
import com.prakash.pwall.service.render.RenderFrame
import com.prakash.pwall.service.render.TimeTransition
import com.prakash.pwall.service.render.WallpaperCoreModule
import com.prakash.pwall.service.render.WallpaperRenderEngine
import com.prakash.pwall.utils.ClockTextFormatter
import com.prakash.pwall.utils.ImageLoader
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Full-bleed preview of the wallpaper. Draws through the same render engine as
 * the live wallpaper (background, glass panel, clock, date) so every premium
 * effect is visible: dynamic colors, breathing, cross-fades, and the cinematic
 * zoom. [clockExtraOffset], when provided, temporarily overrides the clock
 * position (drag & drop).
 */
@Composable
fun WallpaperPreview(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier,
    showHint: Boolean = true,
    clockExtraOffset: Offset? = null
) {
    val engine = remember {
        WallpaperRenderEngine().apply {
            installModule(WallpaperCoreModule())
            installModule(PremiumEffectsModule())
        }
    }

    val imageState by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = settings.selectedImagePath
    ) {
        value = ImageLoader.load(settings.selectedImagePath)
    }
    val palette = remember(imageState) {
        imageState?.let { DominantColorExtractor.extract(it.asAndroidBitmap()) }
    }

    val animated =
        settings.smoothSecondsEnabled ||
            settings.breathingEnabled ||
            settings.fadeTransitionsEnabled ||
            settings.zoomEnabled
    val tick by produceState(initialValue = LocalDateTime.now(), key1 = settings) {
        while (true) {
            value = LocalDateTime.now()
            delay(if (animated) 50L else 1000L)
        }
    }

    val clockText = ClockTextFormatter.formatTime(tick, settings.timeFormat, settings.showSeconds)
    val dateText = ClockTextFormatter.formatDate(tick, settings.dateFormat)
    var prevClock by remember { mutableStateOf<String?>(null) }
    var prevDate by remember { mutableStateOf<String?>(null) }
    var currentClock by remember { mutableStateOf<String?>(null) }
    var currentDate by remember { mutableStateOf<String?>(null) }
    var changeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(clockText, dateText) {
        if (clockText != currentClock || dateText != currentDate) {
            prevClock = currentClock
            prevDate = currentDate
            currentClock = clockText
            currentDate = dateText
            changeMs = System.currentTimeMillis()
        }
    }

    val density = LocalDensity.current.density

    Box(modifier = modifier) {
        val image = imageState
        if (image != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nowMs = System.currentTimeMillis()
                val timeTransition = if (settings.fadeTransitionsEnabled && prevClock != null) {
                    val progress = ((nowMs - changeMs).toFloat() / AnimationMath.TRANSITION_MS)
                        .coerceIn(0f, 1f)
                    if (progress < 1f) {
                        TimeTransition(
                            oldTimeText = prevClock,
                            oldDateText = prevDate,
                            progress = AnimationMath.easeOutCubic(progress)
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }

                val breathing =
                    if (settings.breathingEnabled && settings.breathingStrength > 0f) {
                        Breathing(AnimationMath.breathingPhase(nowMs), settings.breathingStrength)
                    } else {
                        null
                    }

                val cinematicZoom =
                    if (settings.zoomEnabled && settings.zoomStrength > 0f) {
                        CinematicZoom.zoomAt(
                            elapsedMs = nowMs,
                            durationMs = (settings.zoomDurationSeconds * 1000f).toLong(),
                            strength = settings.zoomStrength,
                            direction = settings.zoomDirection
                        )
                    } else {
                        1f
                    }

                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawColor(android.graphics.Color.BLACK)
                    engine.render(
                        canvas.nativeCanvas,
                        RenderFrame(
                            settings = settings,
                            backgroundBitmap = image.asAndroidBitmap(),
                            now = tick,
                            displayDensity = density,
                            width = size.width.toInt(),
                            height = size.height.toInt(),
                            palette = palette,
                            timeTransition = timeTransition,
                            breathing = breathing,
                            cinematicZoom = cinematicZoom
                        )
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PreviewPlaceholderColor),
                contentAlignment = Alignment.Center
            ) {
                if (showHint) {
                    Text(
                        text = "No image selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ClockOverlay(
            settings = settings,
            modifier = Modifier.fillMaxSize(),
            extraOffset = clockExtraOffset
        )
    }
}
