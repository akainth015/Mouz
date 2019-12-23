package me.akainth.mouz

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*
import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.*
import kotlin.concurrent.fixedRateTimer

class MouzService : Service() {

    private val listeners = mutableSetOf<(Boolean) -> Unit>()

    override fun onBind(intent: Intent): IBinder {
        return MouzServiceBinder()
    }

    inner class MouzServiceBinder : Binder() {
        fun onStateChange(listener: (Boolean) -> Unit) {
            listeners += listener
            runListeners()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startSensorThread()
            connectToSocket(intent.getStringExtra(SOCKET_ADDRESS)!!)

            isRunning = true
            runListeners()
        }
        return START_NOT_STICKY
    }

    private fun runListeners() {
        listeners.forEach { it(isRunning) }
    }

    private lateinit var handlerThread: HandlerThread
    private lateinit var sensorManager: SensorManager
    private lateinit var listener: MouzSensorEventListener

    private fun startSensorThread() {
        handlerThread =
            HandlerThread(
                "Mouz Accelerometer Processor",
                Process.THREAD_PRIORITY_MORE_FAVORABLE
            ).apply { start() }
        val handler = Handler(handlerThread.looper)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        listener = MouzSensorEventListener()
        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST,
            handler
        )
    }

    private lateinit var webSocket: WebSocket
    private lateinit var timer: Timer

    private fun connectToSocket(address: String) {
        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
            .url(address)
            .build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Failed to connect to $address", t)
                stopSelf()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, String(bytes.toByteArray()))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, text)
            }
        })

        timer = fixedRateTimer("Mouz Socket Manager", period = 100) {
            webSocket.send(listener.position.joinToString())
        }
    }

    override fun onDestroy() {
        isRunning = false
        runListeners()
        sensorManager.unregisterListener(listener)
        handlerThread.quitSafely()

        timer.cancel()
        webSocket.close(1000, "$TAG is being destroyed")
    }

    companion object {
        @JvmStatic
        var isRunning = false
    }
}

private const val TAG = "MouzService"
private const val SOCKET_ADDRESS = "SOCKET ADDRESS"
