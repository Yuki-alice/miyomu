@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package app.miyomu.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.backhandler.BackHandler
import app.miyomu.danmaku.DanmakuState
import app.miyomu.danmaku.matchTrack
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.player.VideoPlayerScreen
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PROGRESS_SAVE_INTERVAL_MILLIS = 5_000L

/**
 * Playback for an online source episode: the stream url and anti-leech headers come from the
 * source's `resolve`, and progress is stored per (itemId, episodeUrl) like local video.
 */
class OnlineVideoPlayerRoute(
    private val graph: MiyomuGraph,
    private val itemId: String,
    private val entryId: String,
    private val title: String,
    private val uri: String,
    private val headers: Map<String, String> = emptyMap(),
    private val onNextEpisode: (() -> Unit)? = null,
    private val autoPlayNext: Boolean = false,
    private val animeTitle: String? = null,
    private val episodeNumber: Double? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        var danmaku by remember(itemId) { mutableStateOf<DanmakuState?>(null) }
        var resumePosition by remember(itemId) { mutableLongStateOf(0L) }
        var resumeLoaded by remember(itemId) { mutableStateOf(false) }
        var lastSaved by remember(itemId) { mutableLongStateOf(0L) }
        var latestPosition by remember(itemId) { mutableLongStateOf(0L) }
        var latestDuration by remember(itemId) { mutableLongStateOf(0L) }
        var leaving by remember(itemId) { mutableStateOf(false) }

        LaunchedEffect(itemId, entryId) {
            resumePosition = runCatching {
                graph.data.progress.latestFor(itemId, entryId)?.position?.toLong() ?: 0L
            }.getOrDefault(0L)
            lastSaved = resumePosition
            resumeLoaded = true

            val danmakuEnabled = graph.preferenceStore.getBoolean(graph.danmakuKeys.ENABLED, true).get()
            val credentials = graph.readDanmakuCredentials()
            danmaku = if (danmakuEnabled && credentials.isConfigured && animeTitle != null) {
                runCatching {
                    graph.danmaku.setCredentials(credentials)
                    val track = graph.danmaku.matchTrack(animeTitle, episodeNumber)
                    DanmakuState(enabled = true, track = track)
                }.getOrDefault(null)
            } else {
                DanmakuState(enabled = false, track = null)
            }
        }

        suspend fun persistProgress(position: Long, duration: Long) {
            val now = graph.nowMillis()
            graph.data.progress.save(
                ReadingProgress(
                    itemId = itemId,
                    entryId = entryId,
                    kind = ProgressKind.TIMESTAMP,
                    position = position.toDouble(),
                    duration = duration.toDouble(),
                    updatedAt = now,
                    deviceId = graph.deviceId,
                ),
            )
            graph.data.library.markViewed(itemId, now)
        }

        if (!resumeLoaded) return

        val leavePlayer = {
            if (!leaving) {
                leaving = true
                scope.launch {
                    if (latestDuration > 0L) {
                        runCatching { persistProgress(latestPosition, latestDuration) }
                    }
                    navigator.pop()
                }
            }
            Unit
        }

        BackHandler(enabled = true, onBack = leavePlayer)

        VideoPlayerScreen(
            title = title,
            uri = uri,
            startPositionMillis = resumePosition,
            headers = headers,
            initialSpeed = graph.preferenceStore.getFloat(PlayerSpeedKey, 1f).get(),
            onSpeedChanged = { speed -> graph.preferenceStore.getFloat(PlayerSpeedKey, 1f).set(speed) },
            onNextEpisode = onNextEpisode,
            autoPlayNext = autoPlayNext,
            danmaku = danmaku,
            onDanmakuToggled = { enabled ->
                graph.preferenceStore.getBoolean(graph.danmakuKeys.ENABLED, true).set(enabled)
                danmaku = danmaku?.copy(enabled = enabled)
            },
            onBack = leavePlayer,
            onPositionChanged = { position, duration ->
                latestPosition = position
                latestDuration = duration
                if (abs(position - lastSaved) >= PROGRESS_SAVE_INTERVAL_MILLIS) {
                    lastSaved = position
                    scope.launch { runCatching { persistProgress(position, duration) } }
                }
            },
        )
    }
}
