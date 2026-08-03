package com.prakash.pwall.ui.customize

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.service.depth.DiskMaskStore
import com.prakash.pwall.service.depth.MaskKeys
import com.prakash.pwall.ui.components.PreviewPlaceholderColor
import com.prakash.pwall.utils.ImageLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import kotlin.math.roundToInt

/**
 * Full-screen manual depth editor (Prompt 4). Shows the wallpaper with the
 * extracted subject composited on top, and lets the user grow/shrink/feather/
 * smooth the mask before saving it. The edited mask is persisted via
 * [DiskMaskStore] and the wallpaper service reloads it immediately after a save.
 */
@Composable
fun FullScreenMaskEditor(
    settings: WallpaperSettings,
    sourcePath: String?,
    maskStore: DiskMaskStore,
    notifier: MutableSharedFlow<Unit>,
    onDismiss: () -> Unit
) {
    val key = sourcePath?.let { MaskKeys.forImage(it, File(it).lastModified()) }
    val viewModel: MaskEditorViewModel = viewModel(key = sourcePath) {
        MaskEditorViewModel(maskStore, key, sourcePath, notifier)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reload() }

    var expandRadius by remember { mutableFloatStateOf(0f) }
    var shrinkRadius by remember { mutableFloatStateOf(0f) }
    var featherRadius by remember { mutableFloatStateOf(0f) }
    var smoothRadius by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.noMask -> Text(
                    text = state.message ?: "No mask available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
                else -> {
                    MaskEditorPreview(
                        settings = settings,
                        foreground = state.workingForeground,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "Adjust the mask, then Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (state.ready) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    EditorSliderRow(
                        label = "Expand",
                        value = expandRadius,
                        valueRange = 0f..24f,
                        enabled = !state.busy,
                        onValueChange = { expandRadius = it },
                        onValueChangeFinished = { viewModel.expand(expandRadius.roundToInt()) }
                    )
                    EditorSliderRow(
                        label = "Shrink",
                        value = shrinkRadius,
                        valueRange = 0f..24f,
                        enabled = !state.busy,
                        onValueChange = { shrinkRadius = it },
                        onValueChangeFinished = { viewModel.shrink(shrinkRadius.roundToInt()) }
                    )
                    EditorSliderRow(
                        label = "Feather",
                        value = featherRadius,
                        valueRange = 0f..16f,
                        enabled = !state.busy,
                        onValueChange = { featherRadius = it },
                        onValueChangeFinished = { viewModel.feather(featherRadius.roundToInt()) }
                    )
                    EditorSliderRow(
                        label = "Smooth",
                        value = smoothRadius,
                        valueRange = 0f..16f,
                        enabled = !state.busy,
                        onValueChange = { smoothRadius = it },
                        onValueChangeFinished = { viewModel.smooth(smoothRadius.roundToInt()) }
                    )
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
                        TextButton(
                            onClick = viewModel::reset,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                        Button(
                            onClick = {
                                viewModel.save(onSaved = onDismiss)
                            },
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

/**
 * The wallpaper with the (edited) foreground subject composited on top, using
 * the exact same background matrix as the live wallpaper so the mask stays
 * aligned with the image beneath it.
 */
@Composable
private fun MaskEditorPreview(
    settings: WallpaperSettings,
    foreground: Bitmap?,
    modifier: Modifier = Modifier
) {
    val image by produceState<ImageBitmap?>(initialValue = null, settings.selectedImagePath) {
        value = ImageLoader.load(settings.selectedImagePath)
    }

    Canvas(modifier = modifier) {
        val bitmap = image?.asAndroidBitmap()
        if (bitmap == null) {
            drawRect(color = PreviewPlaceholderColor)
            return@Canvas
        }
        val matrix = WallpaperRenderer.backgroundMatrix(
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height,
            targetW = size.width.toInt(),
            targetH = size.height.toInt(),
            mode = settings.backgroundMode,
            zoom = settings.backgroundZoom,
            rotationDegrees = settings.backgroundRotationDegrees,
            translateXFraction = settings.backgroundTranslateXFraction,
            translateYFraction = settings.backgroundTranslateYFraction
        )
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawColor(android.graphics.Color.BLACK)
            canvas.nativeCanvas.drawBitmap(bitmap, matrix, null)
            foreground?.let { canvas.nativeCanvas.drawBitmap(it, matrix, null) }
        }
    }
}

@Composable
private fun EditorSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.width(64.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.roundToInt().toString(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.width(28.dp)
        )
    }
}
