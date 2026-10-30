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
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.model.LibraryItem
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.player.VideoPlayerScreen
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.*
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Position is persisted at most this often, so the position flow does not hammer Room. */
private const val PROGRESS_SAVE_INTERVAL_MILLIS = 5_000L

internal const val PlayerSpeedKey = "player_speed"
internal const val PlayerAutoNextKey = "player_auto_next"

/**
 * Video playback route: resumes from the saved timestamp and writes it back while playing.
 * Replaces Aniyomi's `PlayerActivity` with a Compose screen (ADR-0011, Phase B).
 */
@Composable
fun VideoPlayerRoute(
    graph: MiyomuGraph,
    book: LocalBook,
    episodeLocation: String? = null,
    headers: Map<String, String> = emptyMap(),
) {
    val navigator = LocalMiyomuNavigator.current
        val scope = rememberCoroutineScope()
        val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }
        val entryId = episodeLocation ?: book.location

        var episodes by remember(book.id) { mutableStateOf<List<LocalChapter>>(emptyList()) }
        val episode = remember(episodes, entryId) { episodes.firstOrNull { it.location == entryId } }
        val title = episode?.title ?: book.title
        var danmaku by remember(book.id) { mutableStateOf<DanmakuState?>(null) }
        var resumePosition by remember(book.id) { mutableLongStateOf(0L) }
        var resumeLoaded by remember(book.id) { mutableStateOf(false) }
        var lastSaved by remember(book.id) { mutableLongStateOf(0L) }
        var latestPosition by remember(book.id) { mutableLongStateOf(0L) }
        var latestDuration by remember(book.id) { mutableLongStateOf(0L) }
        var leaving by remember(book.id) { mutableStateOf(false) }

        LaunchedEffect(book.id, entryId) {
            resumePosition = runCatching {
                graph.data.progress.latestFor(itemId, entryId)?.position?.toLong() ?: 0L
            }.getOrDefault(0L)
            lastSaved = resumePosition
            episodes = runCatching { graph.comics.chapters(book) }.getOrDefault(emptyList())
            resumeLoaded = true

            // Danmaku: enabled pref + configured dandanplay credentials + auto-match by title.
            val danmakuEnabled = graph.preferenceStore.getBoolean(graph.danmakuKeys.ENABLED, true).get()
            val credentials = graph.readDanmakuCredentials()
            danmaku = if (danmakuEnabled && credentials.isConfigured) {
                runCatching {
                    graph.danmaku.setCredentials(credentials)
                    val number = episode?.title?.let { t ->
                        app.miyomu.danmaku.parseDanmakuEpisodeNumber(t)
                    } ?: episodes.indexOfFirst { it.location == entryId }
                        .takeIf { it >= 0 }?.plus(1)?.toDouble()
                    val track = graph.danmaku.matchTrack(book.title, number)
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

        // Waiting for the saved position keeps playback from starting at 0 and then jumping.
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

        val nextEpisode = episodes.firstOrNull { chapter ->
            episodes.indexOfFirst { it.location == entryId } >= 0 &&
                episodes.indexOf(chapter) == episodes.indexOfFirst { it.location == entryId } + 1
        }
        VideoPlayerScreen(
            title = title,
            uri = entryId,
            startPositionMillis = resumePosition,
            headers = headers,
            initialSpeed = graph.preferenceStore.getFloat(PlayerSpeedKey, 1f).get(),
            onSpeedChanged = { speed -> graph.preferenceStore.getFloat(PlayerSpeedKey, 1f).set(speed) },
            onNextEpisode = nextEpisode?.let { chapter ->
                { navigator.replace(VideoPlayerKey(book.id, chapter?.id)) }
            },
            autoPlayNext = graph.preferenceStore.getBoolean(PlayerAutoNextKey, true).get(),
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
