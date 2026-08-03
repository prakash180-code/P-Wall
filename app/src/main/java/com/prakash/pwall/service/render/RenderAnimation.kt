package com.prakash.pwall.service.render

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cross-fade state for one frame. When the rendered time/date text changes the
 * wallpaper engine keeps drawing the previous text underneath the new one and
 * blends between them over the transition. [progress] is already eased to 0..1.
 */
data class TimeTransition(
    val oldTimeText: String?,
    val oldDateText: String?,
    val progress: Float
)

/**
 * Gentle, continuous breathing pulse for the clock block. [phase] is the current
 * loop position in radians (period ~4s); [strength] is the user setting in 0..1.
 * [scale] and [alpha] stay tiny so the effect reads as alive, not twitchy.
 */
data class Breathing(
    val phase: Float,
    val strength: Float
) {
    val scale: Float
        get() = 1f + strength.coerceIn(0f, 1f) * 0.018f * (0.5f + 0.5f * cos(phase))

    val alpha: Float
        get() = 1f - strength.coerceIn(0f, 1f) * 0.05f * (0.5f + 0.5f * sin(phase))

    companion object {
        /** Sine loop period used to compute [phase] from elapsed time. */
        const val PERIOD_MS = 4_000L
    }
}

/**
 * Frame-paced timing helpers for the premium micro-animations. Pure so the
 * behavior can be unit tested.
 */
object AnimationMath {

    /** Cross-fade duration after a text change. */
    const val TRANSITION_MS = 420L

    /** Ease-out cubic: fast start, slow settle (premium feel). */
    fun easeOutCubic(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return 1f - (1f - x) * (1f - x) * (1f - x)
    }

    /** Breathing phase (radians) for [elapsedMs], period [Breathing.PERIOD_MS]. */
    fun breathingPhase(elapsedMs: Long): Float =
        (elapsedMs % Breathing.PERIOD_MS).toFloat() / Breathing.PERIOD_MS * 2f * PI.toFloat()
}
