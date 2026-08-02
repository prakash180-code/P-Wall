package com.prakash.pwall.service.motion

/**
 * Supplies the current smoothed parallax motion for a frame. The engine asks for
 * the motion once per rendered frame; null means "no movement this frame".
 */
interface MotionSource {
    /** Current smoothed tilt, or null when parallax is disabled/unavailable. */
    fun currentMotion(): MotionFrame?
}
