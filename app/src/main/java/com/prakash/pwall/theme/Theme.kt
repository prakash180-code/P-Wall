package com.prakash.pwall.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.prakash.pwall.data.model.AppTheme

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

/**
 * App theme for the P-Wall UI itself.
 *
 *  * [AppTheme.AUTO] - follows the system dark mode and uses Android's dynamic
 *    wallpaper colors (Material You) on Android 12+.
 *  * [AppTheme.LIGHT] / [AppTheme.DARK] - deterministic P-Wall palettes.
 *  * [AppTheme.CUSTOM] - deterministic palettes with user-selected primary,
 *    secondary and accent colors.
 */
@Composable
fun PWallTheme(
    appTheme: AppTheme = AppTheme.AUTO,
    customPrimaryColor: Long = 0xFF4F5B92.toLong(),
    customSecondaryColor: Long = 0xFF5B5D72.toLong(),
    customAccentColor: Long = 0xFF00897B.toLong(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.AUTO -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.CUSTOM -> isSystemInDarkTheme()
    }

    val colorScheme = when (appTheme) {
        AppTheme.AUTO -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (darkTheme) {
                DarkColors
            } else {
                LightColors
            }
        }

        AppTheme.CUSTOM -> {
            (if (darkTheme) DarkColors else LightColors).copy(
                primary = Color(customPrimaryColor),
                onPrimary = Color(0xFFFFFFFF),
                secondary = Color(customSecondaryColor),
                tertiary = Color(customAccentColor)
            )
        }

        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
