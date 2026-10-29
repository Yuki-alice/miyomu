package app.miyomu.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.compose.MediampPlayerSurface
import org.openani.mediamp.compose.rememberMediampPlayer
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.SubtitleTrack
import org.openani.mediamp.playUri
import org.openani.mediamp.togglePlayWhenReady

private const val ControlsHideDelayMillis = 3_500L
private const val SeekStepMillis = 10_000L

/**
 * Full-screen video playback for a local file.
 *
 * mediamp gives one call site for Android (ExoPlayer) and desktop (MPV); the chrome here is the
 * Compose replacement for Aniyomi's `PlayerActivity` bars. Audio/subtitle tracks and playback
 * speed come from mediamp's optional features and degrade to an empty sheet when a backend does
 * not expose them.
 */
@Composable
fun VideoPlayerScreen(
    title: String,
    uri: String,
    onBack: () -> Unit,
    startPositionMillis: Long = 0L,
    onPositionChanged: (positionMillis: Long, durationMillis: Long) -> Unit = { _, _ -> },
) {
    if (!isVideoPlaybackAvailable()) {
        PlayerUnavailable(onBack = onBack)
        return
    }

    val player = rememberMediampPlayer()
    val playerState by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val properties by player.mediaProperties.collectAsState()
    val duration = properties?.durationMillis ?: 0L

    val speedFeature = remember(player) { player.features[PlaybackSpeed] }
    val speedState = if (speedFeature == null) {
        remember { mutableFloatStateOf(1f) }
    } else {
        speedFeature.valueFlow.collectAsState(speedFeature.value)
    }
    val speed by speedState

    val audioGroup = remember(player) { player.audioTracks }
    val subtitleGroup = remember(player) { player.subtitleTracks }
    val audioCandidatesState = if (audioGroup == null) {
        remember { mutableStateOf(emptyList<AudioTrack>()) }
    } else {
        audioGroup.candidates.collectAsState(emptyList())
    }
    val audioCandidates by audioCandidatesState
    val subtitleCandidatesState = if (subtitleGroup == null) {
        remember { mutableStateOf(emptyList<SubtitleTrack>()) }
    } else {
        subtitleGroup.candidates.collectAsState(emptyList())
    }
    val subtitleCandidates by subtitleCandidatesState
    val selectedAudioState = if (audioGroup == null) {
        remember { mutableStateOf<AudioTrack?>(null) }
    } else {
        audioGroup.selected.collectAsState()
    }
    val selectedAudio by selectedAudioState
    val selectedSubtitleState = if (subtitleGroup == null) {
        remember { mutableStateOf<SubtitleTrack?>(null) }
    } else {
        subtitleGroup.selected.collectAsState()
    }
    val selectedSubtitle by selectedSubtitleState

    var error by remember(uri) { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionNonce by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(-1f) }
    var seekHint by remember { mutableStateOf<String?>(null) }
    var hintNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(uri) {
        error = null
        runCatching {
            player.playUri(uri, playWhenReady = true, startPositionMillis = startPositionMillis)
        }.onFailure { error = it.message ?: "无法打开视频" }
    }

    LaunchedEffect(position, duration) {
        if (duration > 0L) onPositionChanged(position, duration)
    }

    // Aniyomi hides the bars while playback runs; any tap brings them back and restarts the timer.
    LaunchedEffect(controlsVisible, playerState.playWhenReady, interactionNonce, showSettings) {
        if (controlsVisible && playerState.playWhenReady && !showSettings) {
            delay(ControlsHideDelayMillis)
            controlsVisible = false
        }
    }

    LaunchedEffect(hintNonce) {
        if (hintNonce > 0) {
            delay(700)
            seekHint = null
        }
    }

    fun revealControls() {
        controlsVisible = true
        interactionNonce++
    }

    fun seekBy(deltaMillis: Long) {
        val currentDuration = player.mediaProperties.value?.durationMillis ?: 0L
        val target = (player.currentPositionMillis.value + deltaMillis).coerceIn(0L, currentDuration)
        player.seekTo(target)
        seekHint = if (deltaMillis > 0) "+${deltaMillis / 1000} 秒" else "-${-deltaMillis / 1000} 秒"
        hintNonce++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(player) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                        interactionNonce++
                    },
                    onDoubleTap = { offset ->
                        val third = size.width / 3f
                        when {
                            offset.x < third -> {
                                seekBy(-SeekStepMillis)
                                revealControls()
                            }
                            offset.x > size.width - third -> {
                                seekBy(SeekStepMillis)
                                revealControls()
                            }
                            else -> player.togglePlayWhenReady()
                        }
                    },
                )
            },
    ) {
        MediampPlayerSurface(player, Modifier.fillMaxSize())

        when {
            error != null -> CenteredMessage(error.orEmpty())
            playerState.mediaStatus is MediaStatus.Error ->
                CenteredMessage((playerState.mediaStatus as MediaStatus.Error).error.message.orEmpty())
            playerState.mediaStatus is MediaStatus.Opening -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        seekHint?.let { hint ->
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }

        if (controlsVisible) {
            PlayerTopBar(
                title = title,
                onBack = {
                    onPositionChanged(player.currentPositionMillis.value, duration)
                    onBack()
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            PlayerBottomBar(
                positionMillis = if (scrubPosition >= 0f) scrubPosition.toLong() else position,
                durationMillis = duration,
                playWhenReady = playerState.playWhenReady,
                buffering = playerState.isBuffering,
                settingsEnabled = speedFeature != null || audioCandidates.size > 1 || subtitleCandidates.isNotEmpty(),
                onTogglePlay = {
                    player.togglePlayWhenReady()
                    revealControls()
                },
                onOpenSettings = { showSettings = true },
                onScrub = {
                    scrubPosition = it
                    interactionNonce++
                },
                onScrubFinished = {
                    if (scrubPosition >= 0f) player.seekTo(scrubPosition.toLong())
                    scrubPosition = -1f
                    revealControls()
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showSettings) {
            PlayerSettingsSheet(
                speed = speedFeature?.let { speed },
                onSpeedSelected = { option ->
                    speedFeature?.set(option)
                    revealControls()
                },
                audioCandidates = audioCandidates,
                selectedAudio = selectedAudio,
                onAudioSelected = { audioGroup?.select(it) },
                subtitleCandidates = subtitleCandidates,
                selectedSubtitle = selectedSubtitle,
                onSubtitleSelected = { subtitleGroup?.select(it) },
                onDismiss = { showSettings = false },
            )
        }
    }
}

@Composable
private fun PlayerUnavailable(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = Color.White,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "此版本未内置桌面播放器", color = Color.White)
            Text(
                text = "桌面端的 MPV 原生库是 GPL-3.0，与项目的 Apache-2.0 许可证冲突，" +
                    "因此默认不打包。详见 docs/adr/0012-desktop-video-backend-licensing.md。",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.White)
    }
}
