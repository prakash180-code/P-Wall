package com.prakash.pwall.ui.settings

import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.data.model.AppTheme
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.data.repository.SettingsBackup
import com.prakash.pwall.di.LocalAppContainer
import com.prakash.pwall.ui.AppSettingsViewModel
import com.prakash.pwall.ui.components.ChipRow
import com.prakash.pwall.ui.components.ExpandableColorRow
import com.prakash.pwall.ui.components.ExpandableSectionCard
import com.prakash.pwall.ui.components.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val viewModel: AppSettingsViewModel = viewModel {
        AppSettingsViewModel(container.settingsRepository)
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsScreen(
        settings = settings,
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: WallpaperSettings,
    viewModel: AppSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val repository = container.settingsRepository
    val imageStore = container.imageStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    val versionName = remember(context) {
        val pm = context.packageManager
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0).versionName
            }
        }.getOrNull() ?: "1.0.0"
    }

    val imageSizeBytes by produceState<Long?>(
        initialValue = null,
        key1 = settings.selectedImagePath
    ) {
        value = settings.selectedImagePath?.let { path ->
            withContext(Dispatchers.IO) { runCatching { File(path).length() }.getOrNull() }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader()
                            ?.use { it.readText() }
                    }.getOrNull()
                }
                val restored = json?.let { SettingsBackup.decode(it) }
                if (restored != null) {
                    repository.applySettings(restored)
                    snackbarHostState.showSnackbar("Settings restored from backup")
                } else {
                    snackbarHostState.showSnackbar("That file is not a valid P-Wall backup")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExpandableSectionCard(
                    title = "App Theme",
                    summary = settings.appTheme.displayName,
                    defaultExpanded = true
                ) {
                    Text(
                        text = "Colors used by the P-Wall app itself. Auto follows your system and uses Android's dynamic wallpaper colors when available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ChipRow(
                        options = AppTheme.entries.map { it.displayName to (it == settings.appTheme) }
                    ) { index -> viewModel.setAppTheme(AppTheme.entries[index]) }
                    if (settings.appTheme == AppTheme.CUSTOM) {
                        ExpandableColorRow(
                            title = "Primary color",
                            color = androidx.compose.ui.graphics.Color(settings.customPrimaryColor),
                            onColorChange = viewModel::setCustomPrimaryColor
                        )
                        ExpandableColorRow(
                            title = "Secondary color",
                            color = androidx.compose.ui.graphics.Color(settings.customSecondaryColor),
                            onColorChange = viewModel::setCustomSecondaryColor
                        )
                        ExpandableColorRow(
                            title = "Accent color",
                            color = androidx.compose.ui.graphics.Color(settings.customAccentColor),
                            onColorChange = viewModel::setCustomAccentColor
                        )
                    }
                }
            }
            item {
                SectionCard("Backup & Restore") {
                    Text(
                        text = "Back up every setting as a small file you can share or keep, then restore it on any device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val json = withContext(Dispatchers.IO) {
                                    SettingsBackup.encode(settings)
                                }
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "P-Wall settings backup")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                runCatching {
                                    context.startActivity(
                                        Intent.createChooser(send, "Back up P-Wall settings")
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back up settings")
                    }
                    OutlinedButton(
                        onClick = {
                            restoreLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Restore from backup")
                    }
                }
            }
            item {
                SectionCard("Storage") {
                    if (settings.selectedImagePath != null) {
                        Text(
                            text = "Wallpaper image: ${formatBytes(imageSizeBytes ?: 0L)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    runCatching { imageStore.deleteImage() }
                                        .onSuccess {
                                            snackbarHostState.showSnackbar("Wallpaper image cleared")
                                        }
                                        .onFailure {
                                            snackbarHostState.showSnackbar("Could not clear the image")
                                        }
                                }
                            }
                        ) {
                            Text("Clear wallpaper image")
                        }
                    } else {
                        Text(
                            text = "No wallpaper image stored yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SectionCard("About") {
                    Text(
                        text = "P-Wall",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Version $versionName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "A premium live wallpaper app: smooth 3D parallax, cinematic zoom, glass clock and on-device AI depth.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Column {
                    Button(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Reset all settings")
                    }
                    Text(
                        text = "Restores every option to its factory default. The selected image stays.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all settings?") },
            text = { Text("All of your clock, effects and theme choices will return to their defaults. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            repository.resetToDefaults()
                            snackbarHostState.showSnackbar("Settings reset to defaults")
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
