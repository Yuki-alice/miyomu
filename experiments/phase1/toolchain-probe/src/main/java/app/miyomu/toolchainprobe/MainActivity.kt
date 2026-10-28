package app.miyomu.toolchainprobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.quickjs.quickJs
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.openani.mediamp.MediampPlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var jsResult by remember { mutableStateOf("running") }
            var networkResult by remember { mutableStateOf("network=pending") }
            var playerApi by remember { mutableStateOf("resolving") }

            LaunchedEffect(Unit) {
                jsResult = withContext(Dispatchers.Default) {
                    runCatching { quickJs { evaluate<Int>("1 + 2") } }
                        .fold({ "quickjs=$it" }, { "quickjs=error:${it.message}" })
                }
                networkResult = withContext(Dispatchers.IO) {
                    val client = HttpClient(OkHttp)
                    try {
                        val response = client.get("http://10.0.2.2:8765/m0")
                        "ktor=${response.status.value} body=${response.bodyAsText()}"
                    } catch (error: Throwable) {
                        "ktor=error:${error.message}"
                    } finally {
                        client.close()
                    }
                }
                playerApi = "player=${MediampPlayer::class.java.name}"
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(24.dp),
                ) {
                    Text("Miyomu M0", style = MaterialTheme.typography.headlineMedium)
                    Text(jsResult)
                    Text(networkResult)
                    Text(playerApi)
                }
            }
        }
    }
}
