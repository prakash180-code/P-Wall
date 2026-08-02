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
 *   which is stable and drift-free.
 * - Gyroscope adds a subtle, instantaneous responsiveness on top of the tilt
 *   (angular velocity, never integrated, so there is no drift).
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

    @Volatile
    private var tiltX = 0f

    @Volatile
    private var tiltY = 0f

    @Volatile
    private var lastMotionElapsed = Long.MAX_VALUE

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    /** Normalized tilt components, roughly -1..1, before smoothing/scaling. */
    fun currentTilt(): Pair<Float, Float> = tiltX to tiltY

    /** Elapsed realtime (ms) of the most recent significant movement. */
    fun lastMotionElapsed(): Long = lastMotionElapsed

    fun start() {
        if (!isAvailable || handler != null) return
        val looperThread = HandlerThread("pwall-motion").apply { start() }
        thread = looperThread
        val looperHandler = Handler(looperThread.looper)
        handler = looperHandler
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
        tiltX = 0f
        tiltY = 0f
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
        val deltaX = newTiltX - tiltX
        val deltaY = newTiltY - tiltY
        if (deltaX * deltaX + deltaY * deltaY > MOTION_EPSILON_SQUARED) {
            lastMotionElapsed = SystemClock.elapsedRealtime()
        }
        tiltX = newTiltX
        tiltY = newTiltY
    }

    private fun updateFromGyroscope(values: FloatArray) {
        // Subtle instantaneous emphasis only (no integration -> no drift).
        val rollVelocity = values[2]
        val pitchVelocity = values[0]
        tiltX = (tiltX + rollVelocity * GYRO_EMPHASIS).coerceIn(-1f, 1f)
        tiltY = (tiltY + pitchVelocity * GYRO_EMPHASIS).coerceIn(-1f, 1f)
    }

    private companion object {
        /** Movement detected when squared tilt delta exceeds this value. */
        const val MOTION_EPSILON_SQUARED = 0.0004f

        /** Small gain applied to gyroscope velocity for the subtle emphasis. */
        const val GYRO_EMPHASIS = 0.02f
    }
}
