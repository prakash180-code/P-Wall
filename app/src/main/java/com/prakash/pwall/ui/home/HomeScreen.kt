package com.prakash.pwall.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prakash.pwall.di.LocalAppContainer
import com.prakash.pwall.ui.components.PhoneFramePreview
import com.prakash.pwall.ui.components.PWallIcons
import com.prakash.pwall.ui.components.PressScaleCard
import com.prakash.pwall.ui.customize.launchWallpaperPicker

@Composable
fun HomeRoute(
    onOpenPreview: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenEffects: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(container.settingsRepository, container.imageStore)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
        } else {
            // Fallback to Storage Access Framework picker.
            openDocument.launch("image/*")
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    HomeScreen(
        uiState = uiState,
        onSelectImage = {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onOpenPreview = onOpenPreview,
        onOpenClock = onOpenClock,
        onOpenEffects = onOpenEffects,
        onOpenCustomize = onOpenCustomize,
        onOpenSettings = onOpenSettings,
        onApplyWallpaper = { launchWallpaperPicker(context) },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeViewModel.HomeUiState,
    onSelectImage: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenEffects: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenSettings: () -> Unit,
    onApplyWallpaper: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "P-Wall",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                PhoneFrameHero(
                    uiState = uiState,
                    onSelectImage = onSelectImage,
                    onOpenPreview = onOpenPreview
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = onApplyWallpaper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null)
                    Text(
                        text = "Apply Live Wallpaper",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            item {
                ColorfulActionCard(
                    title = "Clock",
                    subtitle = "Layout, font & colors",
                    icon = PWallIcons.Tune,
                    color = Color(0xFF6750A4),
                    onClick = onOpenClock
                )
            }
            item {
                ColorfulActionCard(
                    title = "Effects",
                    subtitle = "Parallax, glass & zoom",
                    icon = PWallIcons.Wallpaper,
                    color = Color(0xFF00696D),
                    onClick = onOpenEffects
                )
            }
            item {
                ColorfulActionCard(
                    title = "Background",
                    subtitle = "Image & photo editing",
                    icon = PWallIcons.ImagePlaceholder,
                    color = Color(0xFF7D5260),
                    onClick = onOpenCustomize
                )
            }
            item {
                ColorfulActionCard(
                    title = "Settings",
                    subtitle = "Theme, backup & more",
                    icon = PWallIcons.Tune,
                    color = Color(0xFF386A20),
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun PhoneFrameHero(
    uiState: HomeViewModel.HomeUiState,
    onSelectImage: () -> Unit,
    onOpenPreview: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            PhoneFramePreview(
                settings = uiState.settings,
                modifier = Modifier
                    .width(200.dp)
                    .aspectRatio(0.5f)
            ) {
                if (uiState.isSavingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp),
                        color = Color.White
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = onSelectImage,
                enabled = !uiState.isSavingImage,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (uiState.settings.selectedImagePath == null) "Select Image" else "Change Image")
            }
            TextButton(onClick = onOpenPreview) {
                Text("Open full preview")
            }
        }
    }
}

@Composable
private fun ColorfulActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    PressScaleCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        containerColor = color,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
