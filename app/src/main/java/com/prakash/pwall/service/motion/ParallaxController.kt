package com.prakash.pwall.service.motion

import android.content.Context
import android.os.SystemClock
import com.prakash.pwall.data.model.WallpaperSettings

/**
 * Orchestrates the 3D parallax feature: owns the sensor provider, applies the
 * low-pass smoothing filter and sensitivity/strength scaling, and reports
 * whether the device is moving so the render loop can drop its frame rate when
 * idle (battery optimization).
 *
 * The feature is a no-op when disabled or when the device has no motion sensor.
 */
class ParallaxController(
    context: Context,
    private val provider: SensorMotionProvider = SensorMotionProvider(context)
) : MotionSource {

    val isSupported: Boolean get() = provider.isAvailable

    private var enabled = false
    private var sensitivity = 0.5f
    private var strength = 0.5f
    private var smoothing = 0.5f

    /** True while the wallpaper is visible and the sensors may run. */
    private var visible = false

    @Volatile
    private var tiltX = 0f

    @Volatile
    private var tiltY = 0f

    /**
     * Pushes the latest persisted settings into the controller. When the feature
     * is toggled on/off while the wallpaper is already visible the sensors are
     * started/stopped here too, so enabling parallax takes effect immediately
     * (and a wallpaper that starts before the settings have loaded is still
     * registered once they arrive).
     */
    fun updateSettings(settings: WallpaperSettings) {
        val wasEnabled = enabled
        enabled = settings.parallaxEnabled
        sensitivity = settings.parallaxSensitivityValue.coerceIn(0f, 1f)
        strength = settings.parallaxStrength.coerceIn(0f, 1f)
        smoothing = settings.parallaxSmoothing.coerceIn(0f, 1f)
        if (enabled && !wasEnabled && visible && isSupported) provider.start()
        if (!enabled && wasEnabled && visible) provider.stop()
        if (!enabled) resetTilt()
    }

    /** Starts listening to sensors. No-op unless enabled and supported. */
    fun start() {
        visible = true
        if (!enabled || !isSupported) return
        provider.start()
    }

    /** Stops listening and resets the smoothed state. */
    fun stop() {
        visible = false
        provider.stop()
        resetTilt()
    }

    /** True when the feature can produce motion right now. */
    fun isActive(): Boolean = enabled && isSupported

    /** True when the device moved recently (used to pick the render frame rate). */
    fun isMoving(): Boolean {
        if (!isActive()) return false
        val since = SystemClock.elapsedRealtime() - provider.lastMotionElapsed()
        return since < RECENT_MOTION_MS
    }

    /** Milliseconds since the last significant movement (Long.MAX_VALUE when off). */
    fun idleMilliseconds(): Long {
        if (!isActive()) return Long.MAX_VALUE
        return SystemClock.elapsedRealtime() - provider.lastMotionElapsed()
    }

    override fun currentMotion(): MotionFrame? {
        if (!isActive()) return null
        val (rawX, rawY) = provider.currentTilt()
        tiltX = ParallaxMath.smooth(tiltX, rawX, smoothing)
        tiltY = ParallaxMath.smooth(tiltY, rawY, smoothing)
        return MotionFrame(tiltX, tiltY)
    }

    private fun resetTilt() {
        tiltX = 0f
        tiltY = 0f
    }

    private companion object {
        /** A device is considered "moving" within this window of the last event. */
        const val RECENT_MOTION_MS = 2000L
    }
}
