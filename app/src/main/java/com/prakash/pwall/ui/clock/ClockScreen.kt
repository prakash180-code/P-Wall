package com.prakash.pwall.ui.clock

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.data.model.ClockFont
import com.prakash.pwall.data.model.ClockLayout
import com.prakash.pwall.data.model.DateFormat
import com.prakash.pwall.data.model.PositionPreset
import com.prakash.pwall.data.model.TimeFormat
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.di.LocalAppContainer
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

    ClockScreen(
        settings = settings,
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockScreen(
    settings: WallpaperSettings,
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPositionEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Clock") },
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
                        title = "Layout",
                        summary = settings.clockLayout.displayName,
                        defaultExpanded = true
                    ) {
                        ChipRow(
                            options = ClockLayout.entries.map {
                                it.displayName to (it == settings.clockLayout)
                            }
                        ) { index -> viewModel.setClockLayout(ClockLayout.entries[index]) }
                        Text(
                            text = clockLayoutDescription(settings.clockLayout),
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
                        title = "Font",
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
                    SectionCard("Shadow") {
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
                            text = "Pick a preset or position the clock on a full-screen preview.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showPositionEditor = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Position on full screen")
                        }
                        ChipRow(
                            options = PositionPreset.entries.map {
                                it.displayName to (it == settings.position)
                            }
                        ) { index -> viewModel.setPosition(PositionPreset.entries[index]) }
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
}

private fun clockLayoutDescription(layout: ClockLayout): String = when (layout) {
    ClockLayout.HORIZONTAL -> "Classic single-line time (e.g. 12:45)."
    ClockLayout.VERTICAL_DIGITAL -> "Two-digit digits stacked, seconds below."
    ClockLayout.STACKED_DIGITAL -> "Stacked digits with a large colon separator."
    ClockLayout.COMPACT_VERTICAL -> "Compact stack with an AM/PM marker (24-hour shows seconds instead)."
}
