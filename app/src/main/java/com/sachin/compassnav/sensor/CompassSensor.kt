package com.sachin.compassnav.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CompassSensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading

    // Low-pass filter coefficients
    private val alpha = 0.12f // Smoothing factor: lower = smoother but slower response
    private var smoothSin = 0f
    private var smoothCos = 0f
    private var isFirstSample = true

    fun start() {
        rotationVectorSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        isFirstSample = true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        // Azimuth (rotation around the Z-axis). It's in radians, pointing North.
        val azimuth = orientation[0]

        // Smooth heading using trigonometric low-pass filter to handle boundary wrap-around (360 -> 0)
        val currentSin = sin(azimuth)
        val currentCos = cos(azimuth)

        if (isFirstSample) {
            smoothSin = currentSin
            smoothCos = currentCos
            isFirstSample = false
        } else {
            smoothSin += alpha * (currentSin - smoothSin)
            smoothCos += alpha * (currentCos - smoothCos)
        }

        // Convert back to degrees and normalize to 0..360
        var degrees = Math.toDegrees(atan2(smoothSin.toDouble(), smoothCos.toDouble())).toFloat()
        degrees = (degrees + 360f) % 360f

        _heading.value = degrees
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for basic orientation
    }
}
