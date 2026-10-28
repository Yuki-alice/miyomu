package org.projectv.spike.player

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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.compose.rememberMediampPlayer
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.playUri
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.Subtitle
import org.openani.mediamp.source.UriMediaData
import org.openani.mediamp.togglePlayWhenReady
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "S1 · mediamp desktop",
        state = rememberWindowState(width = 1280.dp, height = 800.dp),
    ) {
        MaterialTheme {
            val path = System.getProperty("fixture.video")
                ?: "spikes/s1-player/fixtures/base.mp4"
            PlayerScreen(path)
        }
    }
}

@Composable
private fun PlayerScreen(path: String) {
    val player = rememberMediampPlayer()
    val state by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    val file = remember(path) { File(path).absoluteFile }
    val subtitleFile = remember {
        System.getProperty("fixture.subtitle")?.let(::File)?.absoluteFile
    }

    suspend fun startPlayback() {
        val sub = subtitleFile?.takeIf { it.exists() }
        if (sub != null) {
            val mime = if (sub.extension.equals("ass", true) || sub.extension.equals("ssa", true)) {
                "text/x-ssa"
            } else {
                "application/x-subrip"
            }
            println("MEDIAMP_SUBTITLE ${sub.absolutePath} mime=$mime")
            player.setMediaData(
                UriMediaData(
                    file.absolutePath,
                    emptyMap(),
                    MediaExtraFiles(
                        listOf(Subtitle(sub.absolutePath, mime, "en", "English")),
                    ),
                ),
                true,
                0L,
            )
        } else {
            player.playUri(file.absolutePath, true, 0L)
        }
    }

    LaunchedEffect(player, path) {
        println("MEDIAMP_FIXTURE ${file.absolutePath} exists=${file.exists()}")
        runCatching {
            startPlayback()
        }.onFailure {
            error = it.message
            println("MEDIAMP_ERROR ${it.stackTraceToString()}")
        }
    }

    LaunchedEffect(player) {
        player.state.collect { current ->
            println(
                "MEDIAMP_STATE status=${current.mediaStatus} " +
                    "playWhenReady=${current.playWhenReady} " +
                    "buffering=${current.isBuffering} playing=${current.isPlaying}",
            )
        }
    }

    LaunchedEffect(player) {
        while (true) {
            println("MEDIAMP_POSITION ${player.currentPositionMillis.value}")
            delay(1_000)
        }
    }

    LaunchedEffect(player) {
        val exitAfterMs = System.getProperty("spike.exitAfterMs")?.toLongOrNull() ?: return@LaunchedEffect
        delay(exitAfterMs)
        println("SPIKE_EXIT after ${exitAfterMs}ms")
        kotlin.system.exitProcess(0)
    }

    LaunchedEffect(player) {
        if (System.getProperty("spike.autoTest") != "true") return@LaunchedEffect
        println("AUTO_TEST_START")
        delay(4_000)
        println(
            "AUTO_TEST_BEFORE_SEEK pos=${player.currentPositionMillis.value} " +
                "duration=${player.mediaProperties.value?.durationMillis}",
        )
        player.seekTo(20_000L)
        delay(1_500)
        println("AUTO_TEST_AFTER_SEEK pos=${player.currentPositionMillis.value}")
        val speed = player.features[PlaybackSpeed]
        println("AUTO_TEST_PLAYBACK_SPEED_SUPPORTED=${speed != null}")
        speed?.set(1.5f)
        delay(1_500)
        println("AUTO_TEST_AFTER_SPEED pos=${player.currentPositionMillis.value}")
        player.togglePlayWhenReady()
        delay(800)
        println("AUTO_TEST_AFTER_PAUSE state=${player.state.value}")
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
                    player.features[PlaybackSpeed]?.set(1.5f)
                }) {
                    Text("1.5x")
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { startPlayback() }.onFailure {
                            error = it.message
                            println("MEDIAMP_ERROR ${it.stackTraceToString()}")
                        }
                    }
                }) {
                    Text("Reload + SRT")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${formatMillis(position)} · ${state.mediaStatus}" +
                        if (state.isBuffering) " · buffering" else "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
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
