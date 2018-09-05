package ga.akainth015.mouz

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var mouzThreadIsRunning = false
    private lateinit var mouzThread: MouzThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mouzThread = MouzThread(this)

        start_stop.setOnClickListener {
            when (mouzThreadIsRunning) {
                true -> {
                    mouzThread.interrupt()
                    start_stop.text = getString(R.string.start)
                }
                false -> {
                    mouzThread.start()
                    start_stop.text = getString(R.string.stop)
                }
            }
            mouzThreadIsRunning = !mouzThreadIsRunning
        }
    }
}
