package com.prakash.pwall.service.motion

/**
 * Immutable per-frame motion snapshot injected into [com.prakash.pwall.service.render.RenderFrame].
 * Values are already smoothed, sensitivity/strength-scaled, and in normalized
 * units; layers convert them to pixels using their own amplitude logic.
 */
data class MotionFrame(
    val tiltX: Float,
    val tiltY: Float
) {
    companion object {
        val NONE = MotionFrame(0f, 0f)
    }
}
