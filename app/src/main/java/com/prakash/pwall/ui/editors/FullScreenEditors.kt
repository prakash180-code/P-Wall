package com.prakash.pwall.ui.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.ui.components.WallpaperPreview
import com.prakash.pwall.utils.ImageLoader
import kotlin.math.roundToInt

/**
 * Full-screen drag & tap editor for the clock position. Covers the whole
 * screen exactly like the live preview so pointer fractions map 1:1 with the
 * rendered wallpaper (no small-box offset issues). Dragging only updates the
 * preview; the position is saved when the user taps "Done".
 */
@Composable
fun FullScreenPositionEditor(
    settings: WallpaperSettings,
    onDone: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    var pendingFraction by remember { mutableStateOf<Offset?>(null) }
    var editorSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { editorSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> dragOffset = offset },
                        onDrag = { change, _ ->
                            change.consume()
                            dragOffset = change.position
                        },
                        onDragEnd = {
                            dragOffset?.let { position ->
                                if (editorSize.width > 0 && editorSize.height > 0) {
                                    pendingFraction = Offset(
                                        position.x / editorSize.width,
                                        position.y / editorSize.height
                                    )
                                }
                            }
                            dragOffset = null
                        },
                        onDragCancel = { dragOffset = null }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (editorSize.width > 0 && editorSize.height > 0) {
                            pendingFraction = Offset(
                                offset.x / editorSize.width,
                                offset.y / editorSize.height
                            )
                        }
                    }
                }
        ) {
            WallpaperPreview(
                settings = settings,
                modifier = Modifier.fillMaxSize(),
                showHint = false,
                clockExtraOffset = pendingFraction?.let { fraction ->
                    Offset(
                        fraction.x * editorSize.width,
                        fraction.y * editorSize.height
                    )
                } ?: dragOffset
            )
            Text(
                text = if (settings.position == PositionPreset.CUSTOM) {
                    "Custom position - tap or drag to move"
                } else {
                    "Tap or drag the clock to a custom position"
                },
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        pendingFraction?.let { fraction ->
                            onDone(fraction.x, fraction.y)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

/**
 * Full-screen editor for the custom background transform. Shows the wallpaper
 * at full size (like the live preview) so zoom/rotate/pan edits render 1:1.
 * Pinch to zoom, drag with one finger to pan, twist two fingers to rotate, or
 * use the sliders; "Done" commits to CUSTOM mode.
 */
@Composable
fun FullScreenBackgroundEditor(
    settings: WallpaperSettings,
    onDone: (zoom: Float, rotationDegrees: Float, translateX: Float, translateY: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var zoom by remember { mutableFloatStateOf(settings.backgroundZoom) }
    var rotationDegrees by remember { mutableFloatStateOf(settings.backgroundRotationDegrees) }
    var translateX by remember { mutableFloatStateOf(settings.backgroundTranslateXFraction) }
    var translateY by remember { mutableFloatStateOf(settings.backgroundTranslateYFraction) }
    var editorSize by remember { mutableStateOf(IntSize.Zero) }
    val image by produceState<ImageBitmap?>(
        initialValue = null,
        settings.selectedImagePath
    ) {
        value = ImageLoader.load(settings.selectedImagePath)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { editorSize = it }
                    .pointerInput(image) {
                        detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 8f)
                            rotationDegrees =
                                (rotationDegrees + Math.toDegrees(gestureRotation.toDouble()).toFloat())
                                    .coerceIn(-45f, 45f)
                            if (editorSize.width > 0 && editorSize.height > 0 && image != null) {
                                val (maxPanX, maxPanY) = WallpaperRenderer.customPanBounds(
                                    bitmapWidth = image!!.width,
                                    bitmapHeight = image!!.height,
                                    targetW = editorSize.width,
                                    targetH = editorSize.height,
                                    zoom = zoom,
                                    rotationDegrees = rotationDegrees
                                )
                                if (maxPanX > 0f) {
                                    translateX = (translateX + pan.x / maxPanX).coerceIn(-1f, 1f)
                                }
                                if (maxPanY > 0f) {
                                    translateY = (translateY + pan.y / maxPanY).coerceIn(-1f, 1f)
                                }
                            }
                        }
                    }
            ) {
                WallpaperPreview(
                    settings = settings.copy(
                        backgroundMode = BackgroundMode.CUSTOM,
                        backgroundZoom = zoom,
                        backgroundRotationDegrees = rotationDegrees,
                        backgroundTranslateXFraction = translateX,
                        backgroundTranslateYFraction = translateY
                    ),
                    modifier = Modifier.fillMaxSize(),
                    showHint = false
                )
                Text(
                    text = "Pinch to zoom - drag to move - twist to rotate",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zoom ${"%.1f".format(zoom)}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it.coerceIn(1f, 8f) },
                        valueRange = 1f..8f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rotate ${rotationDegrees.roundToInt()}°",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.width(90.dp)
                    )
                    Slider(
                        value = rotationDegrees,
                        onValueChange = { rotationDegrees = it.coerceIn(-45f, 45f) },
                        valueRange = -45f..45f,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onDone(zoom, rotationDegrees, translateX, translateY)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
