package com.prakash.pwall.service.effects

import com.prakash.pwall.data.model.ZoomDirection
import kotlin.math.PI
import kotlin.math.cos

/**
 * Pure math for the cinematic (Ken Burns style) wallpaper zoom. Produces a
 * scale factor that sweeps smoothly between 1.0 and 1 + [strength] over one
 * [durationMs] loop. No Android types so it can be unit tested on the JVM.
 */
object CinematicZoom {

    /**
     * Returns the scale factor for [elapsedMs] into the zoom loop.
     *
     * - [ZoomDirection.ZOOM_IN]: starts at 1.0 and grows to 1 + strength.
     * - [ZoomDirection.ZOOM_OUT]: starts at 1 + strength and shrinks to 1.0.
     * - [ZoomDirection.ALTERNATE]: eases in then out (smooth sine loop).
     */
    fun zoomAt(
        elapsedMs: Long,
        durationMs: Long,
        strength: Float,
        direction: ZoomDirection
    ): Float {
        val s = strength.coerceIn(0f, 1f)
        if (s <= 0f || durationMs <= 0L) return 1f
        val phase = ((elapsedMs % durationMs).toFloat() / durationMs).coerceIn(0f, 1f)
        val delta = when (direction) {
            ZoomDirection.ZOOM_IN -> phase
            ZoomDirection.ZOOM_OUT -> 1f - phase
            ZoomDirection.ALTERNATE -> (1f - cos(2f * PI.toFloat() * phase)) / 2f
        }
        return 1f + s * delta
    }
}
