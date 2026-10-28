package org.projectv.spike.player.android

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.compose.rememberMediampPlayer
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.playUri
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.Subtitle
import org.openani.mediamp.source.UriMediaData
import org.openani.mediamp.togglePlayWhenReady
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fixture = intent.getStringExtra("fixture")
            ?: "file:///sdcard/Download/s1/base.mp4"
        val subtitle = intent.getStringExtra("subtitle")
        val autoTest = intent.getBooleanExtra("autoTest", false)
        val stressSeeks = intent.getBooleanExtra("stressSeeks", false)
        val exitAfterMs = intent.getLongExtra("exitAfterMs", 12_000L)

        setContent {
            MaterialTheme {
                PlayerScreen(
                    fixture = fixture,
                    subtitle = subtitle,
                    autoTest = autoTest,
                    stressSeeks = stressSeeks,
                    exitAfterMs = exitAfterMs,
                    onFinish = { finish() },
                )
            }
        }
    }
}

@Composable
private fun PlayerScreen(
    fixture: String,
    subtitle: String?,
    autoTest: Boolean,
    stressSeeks: Boolean,
    exitAfterMs: Long,
    onFinish: () -> Unit,
) {
    val player = rememberMediampPlayer()
    val state by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    fun mediaUri(value: String): String =
        if (value.contains("://")) value else Uri.fromFile(File(value)).toString()

    LaunchedEffect(player, fixture, subtitle) {
        val videoUri = mediaUri(fixture)
        Log.i(TAG, "S1_ANDROID_FIXTURE $videoUri")
        runCatching {
            if (subtitle != null) {
                val subUri = mediaUri(subtitle)
                val mime = when {
                    subtitle.endsWith(".ass", true) || subtitle.endsWith(".ssa", true) -> "text/x-ssa"
                    else -> "application/x-subrip"
                }
                Log.i(TAG, "S1_ANDROID_SUBTITLE $subUri mime=$mime")
                player.setMediaData(
                    UriMediaData(
                        videoUri,
                        emptyMap(),
                        MediaExtraFiles(listOf(Subtitle(subUri, mime, "en", "English"))),
                    ),
                    true,
                    0L,
                )
            } else {
                player.playUri(videoUri, true, 0L)
            }
        }.onFailure {
            val message = it.message.orEmpty()
            if (message.contains("Player closed", ignoreCase = true)) {
                Log.i(TAG, "S1_ANDROID_CANCELLED $message")
            } else {
                error = message
                Log.e(TAG, "S1_ANDROID_ERROR", it)
            }
        }
    }

    LaunchedEffect(player) {
        player.state.collect { current ->
            Log.i(
                TAG,
                "S1_ANDROID_STATE status=${current.mediaStatus} " +
                    "playWhenReady=${current.playWhenReady} buffering=${current.isBuffering} " +
                    "playing=${current.isPlaying}",
            )
        }
    }

    LaunchedEffect(player) {
        while (true) {
            Log.i(TAG, "S1_ANDROID_POSITION ${player.currentPositionMillis.value}")
            delay(1_000)
        }
    }

    LaunchedEffect(player) {
        if (!autoTest) return@LaunchedEffect
        Log.i(TAG, "S1_ANDROID_AUTO_TEST_START")
        delay(4_000)
        Log.i(TAG, "S1_ANDROID_BEFORE_SEEK pos=${player.currentPositionMillis.value}")
        player.seekTo(20_000L)
        delay(1_500)
        Log.i(TAG, "S1_ANDROID_AFTER_SEEK pos=${player.currentPositionMillis.value}")
        val speed = player.features[PlaybackSpeed]
        Log.i(TAG, "S1_ANDROID_SPEED_SUPPORTED=${speed != null}")
        speed?.set(1.5f)
        delay(1_500)
        Log.i(TAG, "S1_ANDROID_AFTER_SPEED pos=${player.currentPositionMillis.value}")
        player.togglePlayWhenReady()
        delay(800)
        Log.i(TAG, "S1_ANDROID_AFTER_PAUSE state=${player.state.value}")
    }

    LaunchedEffect(player) {
        if (!stressSeeks) return@LaunchedEffect
        delay(2_000)
        val start = System.nanoTime()
        repeat(50) { index ->
            player.seekTo(((index * 1_000L) % 55_000L))
            delay(100)
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000
        Log.i(TAG, "S1_ANDROID_SEEKS_50_DONE elapsed_ms=$elapsed pos=${player.currentPositionMillis.value}")
    }

    LaunchedEffect(Unit) {
        delay(exitAfterMs)
        Log.i(TAG, "S1_ANDROID_FINISH after ${exitAfterMs}ms")
        onFinish()
    }

    DisposableEffect(player) {
        onDispose { player.close() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        MediampPlayerSurface(player, Modifier.fillMaxSize())

        error?.let {
            Text(
                text = "Error: $it",
                color = Color(0xFFFFB4AB),
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(16.dp),
        ) {
            Text(
                text = fixture.substringAfterLast('/'),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.padding(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { player.togglePlayWhenReady() }) {
                    Text(if (state.isPlaying) "Pause" else "Play")
                }
                OutlinedButton(onClick = { player.seekTo((position - 10_000L).coerceAtLeast(0L)) }) {
                    Text("-10s")
                }
                OutlinedButton(onClick = { player.seekTo(position + 10_000L) }) {
                    Text("+10s")
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        player.features[PlaybackSpeed]?.set(1.5f)
                    }
                }) {
                    Text("1.5x")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${formatMillis(position)} · ${state.mediaStatus}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private const val TAG = "S1_ANDROID"
