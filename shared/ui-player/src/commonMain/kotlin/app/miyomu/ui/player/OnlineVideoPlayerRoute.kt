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
import app.miyomu.ui.core.resolveEpisodeStream
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import app.miyomu.ui.navigation.OnlineVideoPlayerKey
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PROGRESS_SAVE_INTERVAL_MILLIS = 5_000L

/**
 * Playback for an online source episode: the stream url and anti-leech headers come from the
 * source's `resolve`, and progress is stored per (itemId, episodeUrl) like local video.
 */
@Composable
fun OnlineVideoPlayerRoute(
    graph: MiyomuGraph,
    route: OnlineVideoPlayerKey,
) {
    val navigator = LocalMiyomuNavigator.current
    val scope = rememberCoroutineScope()

    val episode = route.episodes.getOrNull(route.episodeIndex)
    val itemId = route.itemId
    val entryId = episode?.url.orEmpty()
    val uri = route.uri
    val headers = route.headers
    val animeTitle = route.animeTitle
    val episodeNumber = episode?.number
    val title = listOfNotNull(route.animeTitle, episode?.name).joinToString(" · ")
    val autoPlayNext = graph.preferenceStore.getBoolean(PlayerAutoNextKey, true).get()
    val nextEpisode = route.episodes.getOrNull(route.episodeIndex + 1)

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

        // "Next" resolves the following episode here rather than carrying a callback, which is
        // what keeps the destination a plain serializable key.
        val onNextEpisode: (() -> Unit)? = nextEpisode?.let { next ->
            {
                scope.launch {
                    if (latestDuration > 0L) {
                        runCatching { persistProgress(latestPosition, latestDuration) }
                    }
                    val resolved = resolveEpisodeStream(graph, route.sourceId, next.url)
                    val stream = resolved?.bestStream()
                    if (resolved != null && stream != null) {
                        navigator.replace(
                            route.copy(
                                episodeIndex = route.episodeIndex + 1,
                                uri = stream.url,
                                headers = resolved.headers,
                            ),
                        )
                    }
                }
                Unit
            }
        }

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

