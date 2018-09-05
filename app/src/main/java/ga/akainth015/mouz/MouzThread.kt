package ga.akainth015.mouz

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.TextView
import ga.akainth015.mouz.R.id.server_url
import okhttp3.*

class MouzThread(private val activity: MainActivity) : Thread() {

    private var webSocket: WebSocket? = null

    private val okHttpClient = OkHttpClient()
    private val sensorEventListener = object : SensorEventListener {
        /** The velocity of the phone, derived from the integral of acceleration **/
        var velocity = Array(3) { 0f }

        private val alpha = 0.8f
        private var gravity = Array(3) { 0f }
        private var linear_acceleration = Array(3) { 0f }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d(tag, "${sensor.name}'s accuracy is $accuracy")
        }

        override fun onSensorChanged(event: SensorEvent) {
            // Isolate the force of gravity with the low-pass filter.
            gravity = Array(3) { i -> alpha * gravity[i] + (1 - alpha) * event.values[i] }

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration = Array(3) { i -> event.values[i] - gravity[i] }

            // Take the integral of linear_acceleration to produce a velocity
            velocity = Array(3) { i -> velocity[i] + linear_acceleration[i] }

            webSocket?.send("${velocity[0]} ${velocity[1]}")
        }
    }
    private val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val tag = activity.getString(R.string.tag)
    private val webSocketListener = object : WebSocketListener() {
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            this@MouzThread.webSocket = null
            Log.d(tag, "Closed WebSocket connection because $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            this@MouzThread.webSocket = null
            Log.e(tag, "WebSocket failure for following reason", t)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d(tag, text)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            this@MouzThread.webSocket = webSocket
        }
    }

    override fun interrupt() {
        super.interrupt()
        sensorManager.unregisterListener(sensorEventListener)
        webSocket?.close(1000, "user requested shutdown")

        webSocket = null
        sensorEventListener.velocity = Array(3) { 0f }
    }

    override fun run() {
        super.run()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        val request = Request.Builder()
                .url(activity.findViewById<TextView>(server_url).text.toString())
                .build()
        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
    }
}
