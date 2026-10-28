package org.projectv.spike.js.android

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val status = TextView(this).apply {
            text = "Running S2 Android checks...\n"
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setPadding(32, 48, 32, 32)
        }
        setContentView(ScrollView(this).apply { addView(status) })

        val appContext = applicationContext
        executor.execute {
            val result = runCatching {
                runBlocking { AndroidChecks(appContext).run(BASE_URL) }
            }.getOrElse { error ->
                Log.e("S2_ANDROID", "FAILED", error)
                "S2_ANDROID FAILED\n${error.stackTraceToString()}"
            }
            Log.i("S2_ANDROID", "RESULT\n$result")
            runOnUiThread { status.text = result }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val BASE_URL = "http://10.0.2.2:18080"
    }
}
