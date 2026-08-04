package com.prakash.pwall.service.performance

import com.prakash.pwall.data.model.LowEndPreference
import com.prakash.pwall.data.model.WallpaperSettings

/**
 * Decides whether the device should run in low-end mode and derives the
 * "effective" settings to render with. Pure, JVM-testable.
 *
 * A low-end device skips the most expensive per-frame effects (backdrop blur,
 * breathing, cinematic zoom, digit cross-fade, 3D parallax and text shadows)
 * and the wallpaper is decoded at a smaller size so memory stays within budget.
 */
object LowEndDevice {

    /** Normal longest-edge decode cap for the wallpaper image. */
    const val NORMAL_MAX_DIMENSION = 1920

    /** Low-end longest-edge decode cap (~0.44x the pixels of normal). */
    const val LOW_END_MAX_DIMENSION = 1280

    /** Auto rule: treat devices at or below this ActivityManager memory class as low-end. */
    const val AUTO_MEMORY_CLASS_THRESHOLD_MB = 160

    /**
     * Resolves the low-end flag from the user preference (or the auto rule).
     *
     * @param memoryClassMb the app process memory class in MB
     * @param isLowRamDevice whether the device advertises low-RAM (Android low memory)
     */
    fun decide(
        memoryClassMb: Int,
        isLowRamDevice: Boolean,
        preference: LowEndPreference
    ): Boolean = when (preference) {
        LowEndPreference.ON -> true
        LowEndPreference.OFF -> false
        LowEndPreference.AUTO -> isLowRamDevice || memoryClassMb <= AUTO_MEMORY_CLASS_THRESHOLD_MB
    }

    /**
     * Returns [settings] unchanged on normal hardware, or a copy with the heavy
     * per-frame effects disabled on low-end hardware. Everything else (position,
     * colors, fonts, depth) is preserved so the wallpaper keeps the user's look.
     */
    fun optimizedSettings(settings: WallpaperSettings, lowEnd: Boolean): WallpaperSettings {
        if (!lowEnd) return settings
        return settings.copy(
            glassEnabled = false,
            breathingEnabled = false,
            zoomEnabled = false,
            smoothSecondsEnabled = false,
            fadeTransitionsEnabled = false,
            parallaxEnabled = false,
            shadowEnabled = false
        )
    }
}
