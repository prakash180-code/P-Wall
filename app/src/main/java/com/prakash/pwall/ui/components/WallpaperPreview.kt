package com.prakash.pwall.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.prakash.pwall.data.model.BackgroundMode
import com.prakash.pwall.data.model.WallpaperSettings
import com.prakash.pwall.utils.ImageLoader

/** Maps a background mode to Compose [ContentScale]. */
fun backgroundContentScale(mode: BackgroundMode): ContentScale = when (mode) {
    BackgroundMode.FIT -> ContentScale.Fit
    BackgroundMode.FILL -> ContentScale.Crop
    BackgroundMode.STRETCH -> ContentScale.FillBounds
    BackgroundMode.CENTER_CROP -> ContentScale.Crop
}

/**
 * Full-bleed preview of the wallpaper: selected image with the clock overlay.
 * Any change to [settings] re-renders immediately. [clockExtraOffset], when
 * provided, temporarily overrides the clock position (drag & drop).
 */
@Composable
fun WallpaperPreview(
    settings: WallpaperSettings,
    modifier: Modifier = Modifier,
    showHint: Boolean = true,
    clockExtraOffset: Offset? = null
) {
    val image by produceState<ImageBitmap?>(initialValue = null, settings.selectedImagePath) {
        value = ImageLoader.load(settings.selectedImagePath)
    }

    Box(modifier = modifier) {
        if (image != null) {
            Image(
                bitmap = image!!,
                contentDescription = "Wallpaper background",
                modifier = Modifier.fillMaxSize(),
                contentScale = backgroundContentScale(settings.backgroundMode)
            )
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
