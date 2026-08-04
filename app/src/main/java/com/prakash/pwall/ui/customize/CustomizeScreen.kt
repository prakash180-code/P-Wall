package com.prakash.pwall.ui.customize

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.di.LocalAppContainer
import com.prakash.pwall.service.PWallWallpaperService
import com.prakash.pwall.ui.AppSettingsViewModel
import com.prakash.pwall.ui.components.ChipRow
import com.prakash.pwall.ui.components.SectionCard
import com.prakash.pwall.ui.components.WallpaperPreview
import com.prakash.pwall.ui.editors.FullScreenBackgroundEditor

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
    val viewModel: AppSettingsViewModel = viewModel {
        AppSettingsViewModel(container.settingsRepository)
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
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBackgroundEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Background") },
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
                    .height(280.dp)
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
                    SectionCard("Fill Mode") {
                        ChipRow(
                            options = BackgroundMode.entries.map {
                                it.displayName to (it == settings.backgroundMode)
                            }
                        ) { index ->
                            viewModel.setBackgroundMode(BackgroundMode.entries[index])
                            if (BackgroundMode.entries[index] == BackgroundMode.CUSTOM) {
                                showBackgroundEditor = true
                            }
                        }
                        Text(
                            text = "How the image is scaled onto the screen. Custom lets you zoom, move and rotate the photo exactly how you want it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    SectionCard("Transform") {
                        Text(
                            text = "Fine-tune the photo with pinch-to-zoom, drag and twist gestures on a full-screen preview.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showBackgroundEditor = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit on full screen")
                        }
                        if (settings.backgroundMode != BackgroundMode.CUSTOM) {
                            Text(
                                text = "Editing switches the fill mode to Custom.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
