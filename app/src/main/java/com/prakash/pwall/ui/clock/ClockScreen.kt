package com.prakash.pwall.ui.clock

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.ContainerBorderStyle
import com.prakash.pwall.data.model.ContainerShape
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.DateLayout
import com.prakash.pwall.data.model.GradientDirection
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.TimeLayout
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.model.WidgetBackgroundMode
import com.prakash.pwall.data.model.WidgetStyle
import com.prakash.pwall.di.LocalAppContainer
import com.prakash.pwall.service.render.TimeContainerPreset
import com.prakash.pwall.service.render.TimeContainerPresets
import com.prakash.pwall.service.render.WidgetPreset
import com.prakash.pwall.service.render.WidgetPresets
import com.prakash.pwall.service.render.WidgetStyleRecipe
import com.prakash.pwall.ui.AppSettingsViewModel
import com.prakash.pwall.ui.components.ChipRow
import com.prakash.pwall.ui.components.ExpandableColorRow
import com.prakash.pwall.ui.components.ExpandableSectionCard
import com.prakash.pwall.ui.components.SectionCard
import com.prakash.pwall.ui.components.SliderWithLabel
import com.prakash.pwall.ui.components.SwitchRow
import com.prakash.pwall.ui.components.WallpaperPreview
import com.prakash.pwall.ui.components.dateFormatExample
import com.prakash.pwall.ui.editors.FullScreenPositionEditor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ClockRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val viewModel: AppSettingsViewModel = viewModel {
        AppSettingsViewModel(container.settingsRepository)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun saveWidgetImage(uri: android.net.Uri) {
        runCatching { container.widgetImageStore.saveImage(uri) }
            .onSuccess { path ->
                viewModel.setWidgetImagePath(path)
                snackbarHostState.showSnackbar("Background image set")
            }
            .onFailure { error ->
                snackbarHostState.showSnackbar(error.message ?: "Could not load that image")
            }
    }

    val openWidgetImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) scope.launch { saveWidgetImage(uri) }
    }

    val pickWidgetPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch { saveWidgetImage(uri) }
        } else {
            // Fallback to Storage Access Framework picker.
            openWidgetImage.launch("image/*")
        }
    }

    ClockScreen(
        settings = settings,
        viewModel = viewModel,
        onBack = onBack,
        onPickWidgetImage = {
            pickWidgetPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemoveWidgetImage = {
            scope.launch {
                container.widgetImageStore.deleteImage()
                viewModel.setWidgetImagePath(null)
                snackbarHostState.showSnackbar("Background image removed")
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockScreen(
    settings: WallpaperSettings,
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    onPickWidgetImage: () -> Unit,
    onRemoveWidgetImage: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showPositionEditor by remember { mutableStateOf(false) }
    var showDatePositionEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Clock & Date") },
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
                        title = "Presets",
                        summary = "One-tap Clock & Date themes",
                        defaultExpanded = true
                    ) {
                        Text(
                            text = "Presets restyle the time and date together. Your colors, font and position stay unchanged.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ChipRow(
                            options = WidgetPreset.entries.map {
                                it.displayName to (it == activePreset(settings))
                            }
                        ) { index -> viewModel.applyWidgetPreset(WidgetPreset.entries[index]) }
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Time background",
                        summary = settings.widgetBackgroundMode.displayName
                    ) {
                        Text(
                            text = "Style the panel behind the time only. Transparent (default) keeps the widget directly on the wallpaper.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ChipRow(
                            options = TimeContainerPreset.entries.map {
                                it.displayName to (it == activeTimeContainerPreset(settings))
                            }
                        ) { index -> viewModel.applyTimeContainerPreset(TimeContainerPreset.entries[index]) }
                        ChipRow(
                            options = WidgetBackgroundMode.entries.map {
                                it.displayName to (it == settings.widgetBackgroundMode)
                            }
                        ) { index -> viewModel.setWidgetBackgroundMode(WidgetBackgroundMode.entries[index]) }

                        when (settings.widgetBackgroundMode) {
                            WidgetBackgroundMode.TRANSPARENT -> Text(
                                text = "Nothing is drawn behind the time, so the wallpaper shows through.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            WidgetBackgroundMode.SOLID -> ExpandableColorRow(
                                title = "Fill color",
                                color = settings.widgetBackgroundColorValue,
                                onColorChange = viewModel::setWidgetBackgroundColor
                            )
                            WidgetBackgroundMode.GRADIENT -> {
                                ExpandableColorRow(
                                    title = "Color 1",
                                    color = settings.widgetBackgroundColorValue,
                                    onColorChange = viewModel::setWidgetBackgroundColor
                                )
                                ExpandableColorRow(
                                    title = "Color 2",
                                    color = settings.widgetBackgroundColor2Value,
                                    onColorChange = viewModel::setWidgetBackgroundColor2
                                )
                                ChipRow(
                                    options = GradientDirection.entries.map {
                                        it.displayName to (it == settings.widgetGradientDirection)
                                    }
                                ) { index -> viewModel.setWidgetGradientDirection(GradientDirection.entries[index]) }
                            }
                            WidgetBackgroundMode.GLASS -> {
                                SliderWithLabel(
                                    label = "Blur strength",
                                    value = settings.widgetGlassBlur,
                                    valueRange = 0f..100f,
                                    displayValue = settings.widgetGlassBlur.roundToInt().toString(),
                                    onValueChange = viewModel::setWidgetGlassBlur
                                )
                                ExpandableColorRow(
                                    title = "Tint color",
                                    color = settings.widgetGlassTintColorValue,
                                    onColorChange = viewModel::setWidgetGlassTintColor
                                )
                            }
                            WidgetBackgroundMode.WALLPAPER_BLUR -> {
                                SliderWithLabel(
                                    label = "Blur strength",
                                    value = settings.widgetWallpaperBlurStrength,
                                    valueRange = 0f..100f,
                                    displayValue = settings.widgetWallpaperBlurStrength.roundToInt().toString(),
                                    onValueChange = viewModel::setWidgetWallpaperBlur
                                )
                                ExpandableColorRow(
                                    title = "Tint color",
                                    color = settings.widgetWallpaperTintColorValue,
                                    onColorChange = viewModel::setWidgetWallpaperTintColor
                                )
                            }
                            WidgetBackgroundMode.IMAGE -> {
                                Button(
                                    onClick = onPickWidgetImage,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        if (settings.widgetImagePath == null) {
                                            "Choose image"
                                        } else {
                                            "Replace image"
                                        }
                                    )
                                }
                                if (settings.widgetImagePath != null) {
                                    TextButton(
                                        onClick = onRemoveWidgetImage,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Remove image")
                                    }
                                }
                                ChipRow(
                                    options = BackgroundMode.entries.map {
                                        it.displayName to (it == settings.widgetImageFit)
                                    }
                                ) { index -> viewModel.setWidgetImageFit(BackgroundMode.entries[index]) }
                            }
                        }

                        SliderWithLabel(
                            label = "Opacity",
                            value = settings.widgetBackgroundOpacity.toFloat(),
                            valueRange = 0f..100f,
                            displayValue = "${settings.widgetBackgroundOpacity}%",
                            onValueChange = { viewModel.setWidgetBackgroundOpacity(it.roundToInt()) }
                        )
                        ChipRow(
                            options = ContainerShape.entries.map {
                                it.displayName to (it == settings.widgetShape)
                            }
                        ) { index -> viewModel.setWidgetShape(ContainerShape.entries[index]) }
                        SliderWithLabel(
                            label = "Corner radius",
                            value = settings.widgetCornerRadius,
                            valueRange = 0f..40f,
                            displayValue = settings.widgetCornerRadius.roundToInt().toString(),
                            onValueChange = viewModel::setWidgetCornerRadius
                        )
                        SliderWithLabel(
                            label = "Padding horizontal",
                            value = settings.widgetPaddingHorizontal,
                            valueRange = 0f..50f,
                            displayValue = settings.widgetPaddingHorizontal.roundToInt().toString(),
                            onValueChange = viewModel::setWidgetPaddingHorizontal
                        )
                        SliderWithLabel(
                            label = "Padding vertical",
                            value = settings.widgetPaddingVertical,
                            valueRange = 0f..50f,
                            displayValue = settings.widgetPaddingVertical.roundToInt().toString(),
                            onValueChange = viewModel::setWidgetPaddingVertical
                        )

                        SwitchRow(
                            "Show border",
                            settings.widgetBorderEnabled,
                            viewModel::setWidgetBorderEnabled
                        )
                        if (settings.widgetBorderEnabled) {
                            ExpandableColorRow(
                                title = "Border color",
                                color = settings.widgetBorderColorValue,
                                onColorChange = viewModel::setWidgetBorderColor
                            )
                            SliderWithLabel(
                                label = "Border width",
                                value = settings.widgetBorderWidth,
                                valueRange = 0f..20f,
                                displayValue = settings.widgetBorderWidth.roundToInt().toString(),
                                onValueChange = viewModel::setWidgetBorderWidth
                            )
                            SliderWithLabel(
                                label = "Border opacity",
                                value = settings.widgetBorderOpacity.toFloat(),
                                valueRange = 0f..100f,
                                displayValue = "${settings.widgetBorderOpacity}%",
                                onValueChange = { viewModel.setWidgetBorderOpacity(it.roundToInt()) }
                            )
                            ChipRow(
                                options = ContainerBorderStyle.entries.map {
                                    it.displayName to (it == settings.widgetBorderStyle)
                                }
                            ) { index -> viewModel.setWidgetBorderStyle(ContainerBorderStyle.entries[index]) }
                        }

                        SwitchRow(
                            "Show shadow",
                            settings.widgetShadowEnabled,
                            viewModel::setWidgetShadowEnabled
                        )
                        if (settings.widgetShadowEnabled) {
                            ExpandableColorRow(
                                title = "Shadow color",
                                color = settings.widgetShadowColorValue,
                                onColorChange = viewModel::setWidgetShadowColor
                            )
                            SliderWithLabel(
                                label = "Blur",
                                value = settings.widgetShadowBlur,
                                valueRange = 0f..60f,
                                displayValue = settings.widgetShadowBlur.roundToInt().toString(),
                                onValueChange = viewModel::setWidgetShadowBlur
                            )
                            SliderWithLabel(
                                label = "Spread",
                                value = settings.widgetShadowSpread,
                                valueRange = 0f..40f,
                                displayValue = settings.widgetShadowSpread.roundToInt().toString(),
                                onValueChange = viewModel::setWidgetShadowSpread
                            )
                            SliderWithLabel(
                                label = "Offset X",
                                value = settings.widgetShadowOffsetX,
                                valueRange = -40f..40f,
                                displayValue = settings.widgetShadowOffsetX.roundToInt().toString(),
                                onValueChange = {
                                    viewModel.setWidgetShadowOffset(it, settings.widgetShadowOffsetY)
                                }
                            )
                            SliderWithLabel(
                                label = "Offset Y",
                                value = settings.widgetShadowOffsetY,
                                valueRange = -40f..40f,
                                displayValue = settings.widgetShadowOffsetY.roundToInt().toString(),
                                onValueChange = {
                                    viewModel.setWidgetShadowOffset(settings.widgetShadowOffsetX, it)
                                }
                            )
                        }
                    }
                }
                item {
                    SectionCard("Visibility") {
                        SwitchRow("Show time", settings.clockVisible, viewModel::setClockVisible)
                        SwitchRow("Show date", settings.dateVisible, viewModel::setDateVisible)
                        SwitchRow(
                            "Link date to time",
                            settings.dateLinkedToTime,
                            viewModel::setDateLinkedToTime
                        )
                        if (!settings.dateLinkedToTime) {
                            Text(
                                text = "Unlinked: the date floats at its own position (edit below).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Time layout",
                        summary = settings.timeLayout.displayName,
                        defaultExpanded = true
                    ) {
                        ChipRow(
                            options = TimeLayout.entries.map {
                                it.displayName to (it == settings.timeLayout)
                            }
                        ) { index -> viewModel.setTimeLayout(TimeLayout.entries[index]) }
                        Text(
                            text = timeLayoutDescription(settings.timeLayout),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Time style",
                        summary = settings.timeStyle.displayName
                    ) {
                        ChipRow(
                            options = WidgetStyle.entries.map {
                                it.displayName to (it == settings.timeStyle)
                            }
                        ) { index -> viewModel.setTimeStyle(WidgetStyle.entries[index]) }
                        Text(
                            text = WidgetStyleRecipe.description(settings.timeStyle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Format",
                        summary = settings.timeFormat.displayName
                    ) {
                        ChipRow(
                            options = TimeFormat.entries.map {
                                it.displayName to (it == settings.timeFormat)
                            }
                        ) { index -> viewModel.setTimeFormat(TimeFormat.entries[index]) }
                        SwitchRow("Show seconds", settings.showSeconds, viewModel::setShowSeconds)
                        Text(
                            text = "Date example: ${dateFormatExample(settings.dateFormat)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ChipRow(
                            options = DateFormat.entries.map {
                                it.displayName to (it == settings.dateFormat)
                            }
                        ) { index -> viewModel.setDateFormat(DateFormat.entries[index]) }
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Time font",
                        summary = settings.clockFont.displayName
                    ) {
                        ChipRow(
                            options = ClockFont.entries.map {
                                it.displayName to (it == settings.clockFont)
                            }
                        ) { index -> viewModel.setClockFont(ClockFont.entries[index]) }
                        SliderWithLabel(
                            label = "Size",
                            value = settings.clockFontSizeSp,
                            valueRange = 24f..96f,
                            displayValue = settings.clockFontSizeSp.roundToInt().toString(),
                            onValueChange = viewModel::setClockFontSize
                        )
                        SwitchRow("Bold", settings.clockBold, viewModel::setClockBold)
                        SwitchRow("Italic", settings.clockItalic, viewModel::setClockItalic)
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Date layout",
                        summary = settings.dateLayout.displayName,
                        defaultExpanded = true
                    ) {
                        ChipRow(
                            options = DateLayout.entries.map {
                                it.displayName to (it == settings.dateLayout)
                            }
                        ) { index -> viewModel.setDateLayout(DateLayout.entries[index]) }
                        Text(
                            text = dateLayoutDescription(settings.dateLayout),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SliderWithLabel(
                            label = "Gap below time",
                            value = settings.dateGapMultiplier,
                            valueRange = 0.5f..3f,
                            displayValue = "${"%.2f".format(settings.dateGapMultiplier)}x",
                            onValueChange = viewModel::setDateGapMultiplier
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Date style",
                        summary = settings.dateStyle.displayName
                    ) {
                        ChipRow(
                            options = WidgetStyle.entries.map {
                                it.displayName to (it == settings.dateStyle)
                            }
                        ) { index -> viewModel.setDateStyle(WidgetStyle.entries[index]) }
                        Text(
                            text = WidgetStyleRecipe.description(settings.dateStyle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Date font",
                        summary = settings.dateFont.displayName
                    ) {
                        ChipRow(
                            options = ClockFont.entries.map {
                                it.displayName to (it == settings.dateFont)
                            }
                        ) { index -> viewModel.setDateFont(ClockFont.entries[index]) }
                        SliderWithLabel(
                            label = "Size (0 = auto)",
                            value = settings.dateFontSizeSp,
                            valueRange = 0f..48f,
                            displayValue = if (settings.dateFontSizeSp <= 0f) {
                                "Auto"
                            } else {
                                settings.dateFontSizeSp.roundToInt().toString()
                            },
                            onValueChange = viewModel::setDateFontSize
                        )
                        SwitchRow("Bold", settings.dateBold, viewModel::setDateBold)
                        SwitchRow("Italic", settings.dateItalic, viewModel::setDateItalic)
                        SwitchRow("Animate", settings.dateAnimated, viewModel::setDateAnimated)
                    }
                }
                item {
                    ExpandableSectionCard(
                        title = "Date shadow",
                        summary = if (settings.dateShadowEnabled) "On" else "Off"
                    ) {
                        SwitchRow(
                            "Enable shadow",
                            settings.dateShadowEnabled,
                            viewModel::setDateShadowEnabled
                        )
                        SliderWithLabel(
                            label = "Blur",
                            value = settings.dateShadowBlurRadius,
                            valueRange = 0f..30f,
                            displayValue = settings.dateShadowBlurRadius.roundToInt().toString(),
                            onValueChange = viewModel::setDateShadowBlurRadius
                        )
                        SliderWithLabel(
                            label = "Offset X",
                            value = settings.dateShadowOffsetX,
                            valueRange = -20f..20f,
                            displayValue = settings.dateShadowOffsetX.roundToInt().toString(),
                            onValueChange = {
                                viewModel.setDateShadowOffset(it, settings.dateShadowOffsetY)
                            }
                        )
                        SliderWithLabel(
                            label = "Offset Y",
                            value = settings.dateShadowOffsetY,
                            valueRange = -20f..20f,
                            displayValue = settings.dateShadowOffsetY.roundToInt().toString(),
                            onValueChange = {
                                viewModel.setDateShadowOffset(settings.dateShadowOffsetX, it)
                            }
                        )
                        ExpandableColorRow(
                            title = "Shadow color",
                            color = settings.dateShadowColorValue,
                            onColorChange = viewModel::setDateShadowColor
                        )
                    }
                }
                item {
                    SectionCard("Colors") {
                        ExpandableColorRow(
                            title = "Clock color",
                            color = settings.clockColorValue,
                            onColorChange = viewModel::setClockColor
                        )
                        ExpandableColorRow(
                            title = "Date color",
                            color = settings.dateColorValue,
                            onColorChange = viewModel::setDateColor
                        )
                    }
                }
                item {
                    SectionCard("Time shadow") {
                        SwitchRow("Enable shadow", settings.shadowEnabled, viewModel::setShadowEnabled)
                        SliderWithLabel(
                            label = "Blur",
                            value = settings.shadowBlurRadius,
                            valueRange = 0f..30f,
                            displayValue = settings.shadowBlurRadius.roundToInt().toString(),
                            onValueChange = viewModel::setShadowBlurRadius
                        )
                        SliderWithLabel(
                            label = "Offset X",
                            value = settings.shadowOffsetX,
                            valueRange = -20f..20f,
                            displayValue = settings.shadowOffsetX.roundToInt().toString(),
                            onValueChange = { viewModel.setShadowOffset(it, settings.shadowOffsetY) }
                        )
                        SliderWithLabel(
                            label = "Offset Y",
                            value = settings.shadowOffsetY,
                            valueRange = -20f..20f,
                            displayValue = settings.shadowOffsetY.roundToInt().toString(),
                            onValueChange = { viewModel.setShadowOffset(settings.shadowOffsetX, it) }
                        )
                        ExpandableColorRow(
                            title = "Shadow color",
                            color = settings.shadowColorValue,
                            onColorChange = viewModel::setShadowColor
                        )
                    }
                }
                item {
                    SectionCard("Opacity") {
                        SliderWithLabel(
                            label = "Clock opacity",
                            value = settings.transparency.toFloat(),
                            valueRange = 0f..100f,
                            displayValue = "${settings.transparency}%",
                            onValueChange = { viewModel.setTransparency(it.roundToInt()) }
                        )
                    }
                }
                item {
                    SectionCard("Position") {
                        Text(
                            text = "Position the time widget on a full-screen preview.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showPositionEditor = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Position time on full screen")
                        }
                        ChipRow(
                            options = PositionPreset.entries.map {
                                it.displayName to (it == settings.position)
                            }
                        ) { index -> viewModel.setPosition(PositionPreset.entries[index]) }
                        if (!settings.dateLinkedToTime && settings.dateVisible) {
                            Text(
                                text = "Date is unlinked - position it separately below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showDatePositionEditor = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Position date on full screen")
                            }
                            ChipRow(
                                options = PositionPreset.entries.map {
                                    it.displayName to (it == settings.datePosition)
                                }
                            ) { index -> viewModel.setDatePosition(PositionPreset.entries[index]) }
                        }
                    }
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

    if (showDatePositionEditor) {
        FullScreenPositionEditor(
            settings = settings,
            dateTarget = true,
            onDone = { x, y ->
                viewModel.setDatePosition(PositionPreset.CUSTOM)
                viewModel.setDatePositionFraction(x, y)
                showDatePositionEditor = false
            },
            onDismiss = { showDatePositionEditor = false }
        )
    }
}

/** The preset whose configuration currently matches [settings], if any. */
private fun activePreset(settings: WallpaperSettings): WidgetPreset? =
    WidgetPreset.entries.firstOrNull { preset ->
        WidgetPresets.matches(settings, preset)
    }

/** The preset matching the container, or [TimeContainerPreset.CUSTOM] if tweaked. */
private fun activeTimeContainerPreset(settings: WallpaperSettings): TimeContainerPreset =
    TimeContainerPreset.entries.firstOrNull { preset ->
        TimeContainerPresets.matches(settings, preset)
    } ?: TimeContainerPreset.CUSTOM

private fun timeLayoutDescription(layout: TimeLayout): String = when (layout) {
    TimeLayout.HORIZONTAL -> "Classic single-line time (e.g. 12:45)."
    TimeLayout.VERTICAL -> "Two-digit digits stacked, seconds below."
    TimeLayout.STACKED -> "Stacked digits with a large colon separator."
    TimeLayout.COMPACT -> "Compact stack with an AM/PM marker (24-hour shows seconds instead)."
    TimeLayout.SPLIT -> "Digits split side by side (flip-clock style)."
    TimeLayout.MINIMAL -> "Small, clean single line."
    TimeLayout.CENTERED -> "Single line, centered text."
    TimeLayout.LEFT_ALIGNED -> "Single line, left aligned."
    TimeLayout.RIGHT_ALIGNED -> "Single line, right aligned."
}

private fun dateLayoutDescription(layout: DateLayout): String = when (layout) {
    DateLayout.HORIZONTAL -> "Single line using your chosen date format."
    DateLayout.VERTICAL -> "Day, date and year stacked."
    DateLayout.MONTH_NAME -> "Month on top, date and year below."
    DateLayout.LONG -> "Full weekday and date on one line."
    DateLayout.SHORT -> "Short weekday and month abbreviation."
    DateLayout.DAY_FIRST -> "Date first, year below."
    DateLayout.COMPACT -> "Compact day + month."
}
