package com.prakash.pwall.ui.customize

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.WallpaperManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.di.LocalAppContainer
import com.prakash.pwall.service.PWallWallpaperService
import com.prakash.pwall.service.WallpaperRenderer
import com.prakash.pwall.ui.components.ColorPicker
import com.prakash.pwall.ui.components.WallpaperPreview
import com.prakash.pwall.utils.ImageLoader
import kotlin.math.roundToInt

/** Builds the intent that opens the system live wallpaper picker. */
fun wallpaperPickerIntent(context: Context): Intent {
    return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
        putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, PWallWallpaperService::class.java)
        )
    }
}

/**
 * Opens the system live wallpaper picker. Silently ignores the (very rare)
 * case where no handler exists for [ACTION_CHANGE_LIVE_WALLPAPER].
 */
fun launchWallpaperPicker(context: Context) {
    runCatching { context.startActivity(wallpaperPickerIntent(context)) }
}

@Composable
fun CustomizeRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val viewModel: CustomizeViewModel = viewModel {
        CustomizeViewModel(container.settingsRepository)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    CustomizeScreen(
        settings = settings,
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeScreen(
    settings: WallpaperSettings,
    viewModel: CustomizeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Customize") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        var showPositionEditor by remember { mutableStateOf(false) }
        var showBackgroundEditor by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WallpaperPreview(
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .clickable { showPositionEditor = true },
                showHint = false
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { TimeSection(settings, viewModel) }
                item { DateSection(settings, viewModel) }
                item { FontSection(settings, viewModel) }
                item { ColorSection(settings, viewModel) }
                item { ShadowSection(settings, viewModel) }
                item { TransparencySection(settings, viewModel) }
                item {
                    PositionSection(
                        settings = settings,
                        viewModel = viewModel,
                        onEditPosition = { showPositionEditor = true }
                    )
                }
                item {
                    BackgroundSection(
                        settings = settings,
                        vm = viewModel,
                        onEditBackground = { showBackgroundEditor = true }
                    )
                }
                item { ParallaxSection(settings, viewModel) }
                item {
                    Button(
                        onClick = { launchWallpaperPicker(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Apply Live Wallpaper")
                    }
                }
            }
        }

        if (showPositionEditor) {
            FullScreenPositionEditor(
                settings = settings,
                onDone = { x, y ->
                    viewModel.setPosition(PositionPreset.CUSTOM)
                    viewModel.setPositionFraction(x, y)
                    showPositionEditor = false
                },
                onDismiss = { showPositionEditor = false }
            )
        }

        if (showBackgroundEditor) {
            FullScreenBackgroundEditor(
                settings = settings,
                onDone = { zoom, rotation, translateX, translateY ->
                    viewModel.setBackgroundMode(BackgroundMode.CUSTOM)
                    viewModel.setBackgroundZoom(zoom)
                    viewModel.setBackgroundRotation(rotation)
                    viewModel.setBackgroundTranslation(translateX, translateY)
                    showBackgroundEditor = false
                },
                onDismiss = { showBackgroundEditor = false }
            )
        }
    }
}

/**
 * Full-screen drag & tap editor for the clock position. Covers the whole
 * screen exactly like the live preview so pointer fractions map 1:1 with the
 * rendered wallpaper (no small-box offset issues). Dragging only updates the
 * preview; the position is saved when the user taps "Done".
 */
@Composable
private fun FullScreenPositionEditor(
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
private fun FullScreenBackgroundEditor(
    settings: WallpaperSettings,
    onDone: (zoom: Float, rotationDegrees: Float, translateX: Float, translateY: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var zoom by remember { mutableStateOf(settings.backgroundZoom) }
    var rotationDegrees by remember { mutableStateOf(settings.backgroundRotationDegrees) }
    var translateX by remember { mutableStateOf(settings.backgroundTranslateXFraction) }
    var translateY by remember { mutableStateOf(settings.backgroundTranslateYFraction) }
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

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChipRow(
    options: List<Pair<String, Boolean>>,
    onSelect: (Int) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(options) { index, (label, selected) ->
            FilterChip(
                selected = selected,
                onClick = { onSelect(index) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun TimeSection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    SectionCard("Time") {
        ChipRow(
            options = TimeFormat.entries.map { it.displayName to (it == settings.timeFormat) }
        ) { index -> vm.setTimeFormat(TimeFormat.entries[index]) }
        SwitchRow("Show seconds", settings.showSeconds, vm::setShowSeconds)
    }
}

@Composable
private fun DateSection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    SectionCard("Date") {
        Text(
            text = "Example: ${formatExample(settings.dateFormat)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChipRow(
            options = DateFormat.entries.map { it.displayName to (it == settings.dateFormat) }
        ) { index -> vm.setDateFormat(DateFormat.entries[index]) }
    }
}

@Composable
private fun FontSection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    SectionCard("Font") {
        ChipRow(
            options = ClockFont.entries.map { it.displayName to (it == settings.clockFont) }
        ) { index -> vm.setClockFont(ClockFont.entries[index]) }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Size",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${settings.clockFontSizeSp.roundToInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = settings.clockFontSizeSp,
                onValueChange = vm::setClockFontSize,
                valueRange = 24f..96f
            )
        }

        SwitchRow("Bold", settings.clockBold, vm::setClockBold)
        SwitchRow("Italic", settings.clockItalic, vm::setClockItalic)
    }
}

@Composable
private fun ColorSection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    SectionCard("Colors") {
        ExpandableColorRow(
            title = "Clock color",
            color = settings.clockColorValue,
            onColorChange = vm::setClockColor
        )
        ExpandableColorRow(
            title = "Date color",
            color = settings.dateColorValue,
            onColorChange = vm::setDateColor
        )
    }
}

@Composable
private fun ExpandableColorRow(
    title: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            ColorPicker(
                color = color,
                onColorChange = onColorChange,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ShadowSection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    SectionCard("Shadow") {
        SwitchRow("Enable shadow", settings.shadowEnabled, vm::setShadowEnabled)

        SliderWithLabel(
            label = "Blur",
            value = settings.shadowBlurRadius,
            valueRange = 0f..30f,
            displayValue = settings.shadowBlurRadius.roundToInt().toString(),
            onValueChange = vm::setShadowBlurRadius
        )
        SliderWithLabel(
            label = "Offset X",
            value = settings.shadowOffsetX,
            valueRange = -20f..20f,
            displayValue = settings.shadowOffsetX.roundToInt().toString(),
            onValueChange = { vm.setShadowOffset(it, settings.shadowOffsetY) }
        )
        SliderWithLabel(
            label = "Offset Y",
            value = settings.shadowOffsetY,
            valueRange = -20f..20f,
            displayValue = settings.shadowOffsetY.roundToInt().toString(),
            onValueChange = { vm.setShadowOffset(settings.shadowOffsetX, it) }
        )

        ExpandableColorRow(
            title = "Shadow color",
            color = settings.shadowColorValue,
            onColorChange = vm::setShadowColor
        )
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun TransparencySection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    SectionCard("Transparency") {
        SliderWithLabel(
            label = "Opacity",
            value = settings.transparency.toFloat(),
            valueRange = 0f..100f,
            displayValue = "${settings.transparency}%",
            onValueChange = { vm.setTransparency(it.roundToInt()) }
        )
    }
}

@Composable
private fun PositionSection(
    settings: WallpaperSettings,
    viewModel: CustomizeViewModel,
    onEditPosition: () -> Unit
) {
    SectionCard("Position") {
        Text(
            text = "Pick a preset or position the clock on a full-screen preview.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onEditPosition,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Position on full screen")
        }
        ChipRow(
            options = PositionPreset.entries.map { it.displayName to (it == settings.position) }
        ) { index -> viewModel.setPosition(PositionPreset.entries[index]) }
    }
}

@Composable
private fun BackgroundSection(
    settings: WallpaperSettings,
    vm: CustomizeViewModel,
    onEditBackground: () -> Unit
) {
    SectionCard("Background") {
        ChipRow(
            options = BackgroundMode.entries.map { it.displayName to (it == settings.backgroundMode) }
        ) { index ->
            vm.setBackgroundMode(BackgroundMode.entries[index])
            if (BackgroundMode.entries[index] == BackgroundMode.CUSTOM) {
                onEditBackground()
            }
        }
        if (settings.backgroundMode == BackgroundMode.CUSTOM) {
            Text(
                text = "Zoom, move and rotate the photo on a full-screen preview.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onEditBackground,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Edit on full screen")
            }
        }
    }
}

@Composable
private fun ParallaxSection(settings: WallpaperSettings, vm: CustomizeViewModel) {
    val context = LocalContext.current
    val supported = remember(context) { hasMotionSensors(context) }
    SectionCard("3D Parallax") {
        SwitchRow("Enable 3D parallax", settings.parallaxEnabled, vm::setParallaxEnabled)
        if (!supported) {
            Text(
                text = "Not supported on this device - no motion sensors found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SliderWithLabel(
                label = "Sensitivity",
                value = settings.parallaxSensitivity,
                valueRange = 0f..1f,
                displayValue = "${(settings.parallaxSensitivity * 100).roundToInt()}%",
                onValueChange = vm::setParallaxSensitivity
            )
            SliderWithLabel(
                label = "Strength",
                value = settings.parallaxStrength,
                valueRange = 0f..1f,
                displayValue = "${(settings.parallaxStrength * 100).roundToInt()}%",
                onValueChange = vm::setParallaxStrength
            )
            SliderWithLabel(
                label = "Motion smoothing",
                value = settings.parallaxSmoothing,
                valueRange = 0f..1f,
                displayValue = "${(settings.parallaxSmoothing * 100).roundToInt()}%",
                onValueChange = vm::setParallaxSmoothing
            )
        }
    }
}

private fun hasMotionSensors(context: Context): Boolean {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
    return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}

private fun formatExample(format: DateFormat): String {
    return when (format) {
        DateFormat.DAY_MONTH_YEAR -> "15 July 2026"
        DateFormat.NUMERIC_DMY -> "15/07/2026"
        DateFormat.DAY_ONLY -> "Wednesday"
        DateFormat.DAY_DATE -> "Wednesday, 15 July"
    }
}
