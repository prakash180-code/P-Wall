package com.prakash.pwall.ui.effects

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.data.model.LowEndPreference
import com.prakash.pwall.data.model.ParallaxSensitivityLevel
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.ZoomDirection
import com.prakash.pwall.di.LocalAppContainer
import com.prakash.pwall.ui.AppSettingsViewModel
import com.prakash.pwall.ui.components.ChipRow
import com.prakash.pwall.ui.components.ExpandableColorRow
import com.prakash.pwall.ui.components.ExpandableSectionCard
import com.prakash.pwall.ui.components.SectionCard
import com.prakash.pwall.ui.components.SliderWithLabel
import com.prakash.pwall.ui.components.SwitchRow
import com.prakash.pwall.ui.components.WallpaperPreview
import com.prakash.pwall.ui.customize.FullScreenMaskEditor
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EffectsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val viewModel: AppSettingsViewModel = viewModel {
        AppSettingsViewModel(container.settingsRepository)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    EffectsScreen(
        settings = settings,
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffectsScreen(
    settings: WallpaperSettings,
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parallaxSupported = remember(context) { hasMotionSensors(context) }
    val container = LocalAppContainer.current
    var showMaskEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Effects") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WallpaperPreview(
                settings = settings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp)),
                showHint = false
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ExpandableSectionCard(
                        title = "3D Parallax",
                        summary = if (settings.parallaxEnabled) "Enabled" else "Off",
                        defaultExpanded = true
                    ) {
                        SwitchRow(
                            "Enable 3D parallax",
                            settings.parallaxEnabled,
                            viewModel::setParallaxEnabled
                        )
                        if (!parallaxSupported) {
                            Text(
                                text = "Not supported on this device - no motion sensors found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Sensitivity",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            ChipRow(
                                options = ParallaxSensitivityLevel.entries.map {
                                    it.displayName to (it == settings.parallaxSensitivityLevel)
                                }
                            ) { index ->
                                viewModel.setParallaxSensitivityLevel(
                                    ParallaxSensitivityLevel.entries[index]
                                )
                            }
                            SliderWithLabel(
                                label = "Strength",
                                value = settings.parallaxStrength,
                                valueRange = 0f..1f,
                                displayValue = "${(settings.parallaxStrength * 100).roundToInt()}%",
                                onValueChange = viewModel::setParallaxStrength
                            )
                            SliderWithLabel(
                                label = "Motion smoothing",
                                value = settings.parallaxSmoothing,
                                valueRange = 0f..1f,
                                displayValue = "${(settings.parallaxSmoothing * 100).roundToInt()}%",
                                onValueChange = viewModel::setParallaxSmoothing
                            )
                            SwitchRow(
                                "Debug overlay",
                                settings.debugParallax,
                                viewModel::setDebugParallax
                            )
                            Text(
                                text = "Debug exaggerates the motion shift so you can verify the effect on the wallpaper.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    SectionCard("AI Depth") {
                        SwitchRow(
                            "Extract foreground (clock behind subject)",
                            settings.depthEnabled,
                            viewModel::setDepthEnabled
                        )
                        if (settings.depthEnabled && settings.selectedImagePath != null) {
                            Button(
                                onClick = { showMaskEditor = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Edit foreground mask")
                            }
                        }
                        Text(
                            text = "Analyzes the wallpaper once when it changes to separate people, pets and objects, then hides the clock behind them. You can fix the result by expanding, shrinking or smoothing the mask in the editor. Requires the on-device AI model (small download on first use).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Glass Clock",
                        summary = if (settings.glassEnabled) "Enabled" else "Off"
                    ) {
                        SwitchRow("Enable glass panel", settings.glassEnabled, viewModel::setGlassEnabled)
                        Text(
                            text = "Frosted blur, soft glow and a subtle border around the clock. Off keeps the default clean look.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SliderWithLabel(
                            label = "Backdrop blur",
                            value = settings.glassBlurRadius,
                            valueRange = 0f..40f,
                            displayValue = settings.glassBlurRadius.roundToInt().toString(),
                            onValueChange = viewModel::setGlassBlurRadius
                        )
                        SliderWithLabel(
                            label = "Panel opacity",
                            value = settings.glassPanelOpacity.toFloat(),
                            valueRange = 0f..100f,
                            displayValue = "${settings.glassPanelOpacity}%",
                            onValueChange = { viewModel.setGlassPanelOpacity(it.roundToInt()) }
                        )
                        SliderWithLabel(
                            label = "Corner radius",
                            value = settings.glassCornerRadius,
                            valueRange = 0f..48f,
                            displayValue = settings.glassCornerRadius.roundToInt().toString(),
                            onValueChange = viewModel::setGlassCornerRadius
                        )
                        SliderWithLabel(
                            label = "Border width",
                            value = settings.glassBorderWidth,
                            valueRange = 0f..8f,
                            displayValue = String.format(
                                Locale.ROOT,
                                "%.1f",
                                settings.glassBorderWidth
                            ),
                            onValueChange = viewModel::setGlassBorderWidth
                        )
                        ExpandableColorRow(
                            title = "Border color",
                            color = settings.glassBorderColorValue,
                            onColorChange = viewModel::setGlassBorderColor
                        )
                        SliderWithLabel(
                            label = "Glow intensity",
                            value = settings.glassGlowRadius,
                            valueRange = 0f..60f,
                            displayValue = settings.glassGlowRadius.roundToInt().toString(),
                            onValueChange = viewModel::setGlassGlowRadius
                        )
                        ExpandableColorRow(
                            title = "Glow color",
                            color = settings.glassGlowColorValue,
                            onColorChange = viewModel::setGlassGlowColor
                        )
                    }
                }
                item {
                    SectionCard("Dynamic Colors") {
                        SwitchRow(
                            "Match clock color to wallpaper",
                            settings.dynamicClockColor,
                            viewModel::setDynamicClockColor
                        )
                        SwitchRow(
                            "Match date color to wallpaper",
                            settings.dynamicDateColor,
                            viewModel::setDynamicDateColor
                        )
                        Text(
                            text = "Picks the dominant colors from the current image, choosing a readable blend for the text. You can still pick a manual color below; the toggle just switches to automatic.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Micro Animations",
                        summary = if (settings.breathingEnabled) "On" else "Off"
                    ) {
                        SwitchRow(
                            "Fade between digits",
                            settings.fadeTransitionsEnabled,
                            viewModel::setFadeTransitionsEnabled
                        )
                        SwitchRow(
                            "Smooth second sweep",
                            settings.smoothSecondsEnabled,
                            viewModel::setSmoothSecondsEnabled
                        )
                        SwitchRow(
                            "Gentle breathing",
                            settings.breathingEnabled,
                            viewModel::setBreathingEnabled
                        )
                        if (settings.breathingEnabled) {
                            SliderWithLabel(
                                label = "Breathing strength",
                                value = settings.breathingStrength,
                                valueRange = 0f..1f,
                                displayValue = "${(settings.breathingStrength * 100).roundToInt()}%",
                                onValueChange = viewModel::setBreathingStrength
                            )
                        }
                        Text(
                            text = "Small, battery-friendly touches: a short cross-fade when the time changes and a barely-there pulse on the clock block.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Cinematic Zoom",
                        summary = if (settings.zoomEnabled) "On" else "Off"
                    ) {
                        SwitchRow("Enable slow zoom", settings.zoomEnabled, viewModel::setZoomEnabled)
                        if (settings.zoomEnabled) {
                            SliderWithLabel(
                                label = "Zoom strength",
                                value = settings.zoomStrength,
                                valueRange = 0f..1f,
                                displayValue = "${(settings.zoomStrength * 100).roundToInt()}%",
                                onValueChange = viewModel::setZoomStrength
                            )
                            SliderWithLabel(
                                label = "Loop duration",
                                value = settings.zoomDurationSeconds,
                                valueRange = 5f..120f,
                                displayValue = "${settings.zoomDurationSeconds.roundToInt()}s",
                                onValueChange = viewModel::setZoomDurationSeconds
                            )
                            ChipRow(
                                options = ZoomDirection.entries.map {
                                    it.displayName to (it == settings.zoomDirection)
                                }
                            ) { index -> viewModel.setZoomDirection(ZoomDirection.entries[index]) }
                        }
                        Text(
                            text = "A gentle Ken Burns sweep, looping as long as the wallpaper runs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    SectionCard("Performance") {
                        Text(
                            text = "Low-end mode disables the heavy per-frame effects (blur, breathing, zoom, transitions, shadows) and decodes the wallpaper smaller, so the wallpaper stays smooth on weak hardware or while saving battery.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ChipRow(
                            options = LowEndPreference.entries.map {
                                it.displayName to (it == settings.lowEnd)
                            }
                        ) { index -> viewModel.setLowEnd(LowEndPreference.entries[index]) }
                    }
                }
            }
        }
    }

    if (showMaskEditor) {
        FullScreenMaskEditor(
            settings = settings,
            sourcePath = settings.selectedImagePath,
            maskStore = container.maskStore,
            notifier = container.maskEditNotifier,
            onDismiss = { showMaskEditor = false }
        )
    }
}

private fun hasMotionSensors(context: Context): Boolean {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
    return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}
