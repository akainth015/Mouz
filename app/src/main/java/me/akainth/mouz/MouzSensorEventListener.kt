package me.akainth.mouz

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.util.Log

class MouzSensorEventListener : SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(TAG, "${sensor.name} has an accuracy of $accuracy")
    }

    private var timestamp = 0L
    private val acceleration = floatArrayOf(0f, 0f)
    private val velocity = doubleArrayOf(0.0, 0.0)
    val position = doubleArrayOf(0.0, 0.0)

    private val gravity = floatArrayOf(0f, 0f, 0f)

    override fun onSensorChanged(event: SensorEvent) {
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]

        val newAcceleration = floatArrayOf(
            event.values[0] - gravity[0],
            event.values[1] - gravity[1]
        )

        if (timestamp != 0L) {
            /**
             * Seconds elapsed since the last sensor reading
             */
            val dT = (event.timestamp - timestamp) / 1e9

            velocity[0] += (newAcceleration[0] + acceleration[0]) / 2 * dT
            velocity[1] += (newAcceleration[1] + acceleration[1]) / 2 * dT

            position[0] += velocity[0] * dT
            position[1] += velocity[1] * dT
        }

        timestamp = event.timestamp
        acceleration[0] = newAcceleration[0]
        acceleration[1] = newAcceleration[1]
    }
}

private const val TAG = "MouzSensorEventListener"
private const val alpha = 0.8f
