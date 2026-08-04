package com.prakash.pwall.service.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import kotlin.math.atan2

/**
 * Android sensor-backed source of device tilt.
 *
 * - Accelerometer is the primary tilt signal (gravity direction -> pitch/roll),
 *   which is stable and drift-free. It is sampled at [sensorDelay] and, when the
 *   render thread asks between two samples, [currentTilt] interpolates linearly
 *   between them so 60 fps rendering never sees stepped jumps.
 * - Gyroscope adds a subtle, instantaneous responsiveness on top of the tilt
 *   (angular velocity, never integrated, so there is no drift).
 * - A dead-zone suppresses sub-threshold sensor noise so tiny hand movements do
 *   not cause micro-jitter (and do not wake up the faster render loop).
 * - Events are delivered on a dedicated looper thread; the render thread reads
 *   the latest values via volatile fields (no locks, no main-thread work).
 * - Motion timestamps let the render loop drop to a lower frame rate when the
 *   device is still, which keeps battery usage low.
 */
class SensorMotionProvider(
    context: Context,
    private val sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME
) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    /** True when at least one motion sensor exists (feature can run). */
    val isAvailable: Boolean get() = accelerometer != null || gyroscope != null

    /** Latest accelerometer sample; the previous one is kept for interpolation. */
    @Volatile
    private var curAccelX = 0f

    @Volatile
    private var curAccelY = 0f

    @Volatile
    private var prevAccelX = 0f

    @Volatile
    private var prevAccelY = 0f

    @Volatile
    private var prevAccelElapsed = 0L

    @Volatile
    private var curAccelElapsed = 0L

    /** Gyroscope emphasis accumulated since the last accelerometer sample. */
    @Volatile
    private var gyroDX = 0f

    @Volatile
    private var gyroDY = 0f

    @Volatile
    private var lastMotionElapsed = Long.MAX_VALUE

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    /**
     * Normalized tilt components, roughly -1..1, before smoothing/scaling.
     * Interpolated between the two most recent accelerometer samples so frames
     * between sensor events still move smoothly; gyroscope emphasis is blended
     * on top.
     */
    fun currentTilt(): Pair<Float, Float> {
        val now = SystemClock.elapsedRealtime()
        if (curAccelElapsed == 0L) return gyroDX to gyroDY
        val span = curAccelElapsed - prevAccelElapsed
        val frac = if (span > 0L) {
            ((now - prevAccelElapsed).toFloat() / span).coerceIn(0f, 1f)
        } else {
            1f
        }
        val x = prevAccelX + (curAccelX - prevAccelX) * frac + gyroDX
        val y = prevAccelY + (curAccelY - prevAccelY) * frac + gyroDY
        return x.coerceIn(-1f, 1f) to y.coerceIn(-1f, 1f)
    }

    /** Elapsed realtime (ms) of the most recent significant movement. */
    fun lastMotionElapsed(): Long = lastMotionElapsed

    fun start() {
        if (!isAvailable || handler != null) return
        val looperThread = HandlerThread("pwall-motion").apply { start() }
        thread = looperThread
        val looperHandler = Handler(looperThread.looper)
        handler = looperHandler
        // No motion yet: treat the device as idle so the render loop does not
        // spin at 60 fps before the first real movement arrives.
        lastMotionElapsed = SystemClock.elapsedRealtime()
        accelerometer?.let {
            sensorManager.registerListener(listener, it, sensorDelay, looperHandler)
        }
        gyroscope?.let {
            sensorManager.registerListener(listener, it, sensorDelay, looperHandler)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
        handler = null
        thread?.quitSafely()
        thread = null
        curAccelX = 0f
        curAccelY = 0f
        prevAccelX = 0f
        prevAccelY = 0f
        prevAccelElapsed = 0L
        curAccelElapsed = 0L
        gyroDX = 0f
        gyroDY = 0f
        lastMotionElapsed = Long.MAX_VALUE
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> updateFromAccelerometer(event.values)
                Sensor.TYPE_GYROSCOPE -> updateFromGyroscope(event.values)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    private fun updateFromAccelerometer(values: FloatArray) {
        val ax = values[0]
        val ay = values[1]
        val az = values[2]
        // Gravity direction -> pitch/roll in normalized -1..1 units.
        val newTiltX = (atan2(ax.toDouble(), az.toDouble()) / Math.PI * 2).toFloat().coerceIn(-1f, 1f)
        val newTiltY = (atan2(ay.toDouble(), az.toDouble()) / Math.PI * 2).toFloat().coerceIn(-1f, 1f)
        val deltaX = newTiltX - curAccelX
        val deltaY = newTiltY - curAccelY
        // Dead-zone: ignore tiny changes so sensor noise cannot cause jitter or
        // keep the fast render loop awake. Larger movements push a new sample.
        if (deltaX * deltaX + deltaY * deltaY > TILT_DEAD_ZONE_SQUARED) {
            prevAccelX = curAccelX
            prevAccelY = curAccelY
            prevAccelElapsed = curAccelElapsed
            curAccelX = newTiltX
            curAccelY = newTiltY
            curAccelElapsed = SystemClock.elapsedRealtime()
            gyroDX = 0f
            gyroDY = 0f
            if (deltaX * deltaX + deltaY * deltaY > MOTION_EPSILON_SQUARED) {
                lastMotionElapsed = curAccelElapsed
            }
        }
    }

    private fun updateFromGyroscope(values: FloatArray) {
        // Subtle instantaneous emphasis only (no integration -> no drift).
        val rollVelocity = values[2]
        val pitchVelocity = values[0]
        gyroDX = (gyroDX + rollVelocity * GYRO_EMPHASIS).coerceIn(-1f, 1f)
        gyroDY = (gyroDY + pitchVelocity * GYRO_EMPHASIS).coerceIn(-1f, 1f)
        val delta = rollVelocity * rollVelocity + pitchVelocity * pitchVelocity
        if (delta > MOTION_EPSILON_SQUARED) {
            lastMotionElapsed = SystemClock.elapsedRealtime()
        }
    }

    private companion object {
        /** Movement detected when squared tilt delta exceeds this value. */
        const val MOTION_EPSILON_SQUARED = 0.0004f

        /** Squared delta under which a tilt change is treated as sensor noise. */
        const val TILT_DEAD_ZONE_SQUARED = 0.0001f

        /** Small gain applied to gyroscope velocity for the subtle emphasis. */
        const val GYRO_EMPHASIS = 0.02f
    }
}
