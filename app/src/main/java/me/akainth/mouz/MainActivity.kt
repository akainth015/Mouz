package me.akainth.mouz

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.net.MalformedURLException
import java.net.URI
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onDestroy() {
        val serviceIntent = Intent(this, MouzService::class.java)
        stopService(serviceIntent)
        super.onDestroy()
    }

    private fun bindToMouzService() {
        val serviceIntent = Intent(this, MouzService::class.java)
        val serviceConnection = object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(TAG, "${name.className} disconnected")
                resetInterface()
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(TAG, "${name.className} connected")
                (service as MouzService.MouzServiceBinder).apply {
                    onStateChange { isRunning ->
                        if (isRunning) {
                            editSocketAddress.isEnabled = false
                            buttonServiceStatus.text = getString(R.string.stop)
                        } else {
                            resetInterface()
                        }
                    }
                }
            }
        }
        bindService(serviceIntent, serviceConnection, Service.BIND_IMPORTANT)
    }

    fun onStateChangePressed(@Suppress("UNUSED_PARAMETER") view: View) {
        val serviceIntent = Intent(this, MouzService::class.java)
        if (!MouzService.isRunning) {
            try {
                val address = editSocketAddress.text.toString()
                URI(address)
                serviceIntent.putExtra("SOCKET ADDRESS", address)
                startService(serviceIntent)
                bindToMouzService()
            } catch (error: MalformedURLException) {
                Toast.makeText(this, "Invalid socket address", Toast.LENGTH_SHORT).show()
            }
        } else {
            stopService(serviceIntent)
        }
    }

    private fun resetInterface() {
        editSocketAddress.isEnabled = true
        buttonServiceStatus.text = getString(R.string.connect)
    }
}

private const val TAG = "MainActivity"