package com.example.stars1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OrientationManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val orientation = _orientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun register() {
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            _orientation.value = orientationAngles
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
