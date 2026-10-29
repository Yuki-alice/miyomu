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
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.player.VideoPlayerScreen
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Position is persisted at most this often, so the position flow does not hammer Room. */
private const val PROGRESS_SAVE_INTERVAL_MILLIS = 5_000L

/**
 * Video playback route: resumes from the saved timestamp and writes it back while playing.
 * Replaces Aniyomi's `PlayerActivity` with a Compose screen (ADR-0011, Phase B).
 */
class VideoPlayerRoute(
    private val graph: MiyomuGraph,
    private val book: LocalBook,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }

        var resumePosition by remember(book.id) { mutableLongStateOf(0L) }
        var resumeLoaded by remember(book.id) { mutableStateOf(false) }
        var lastSaved by remember(book.id) { mutableLongStateOf(0L) }
        var latestPosition by remember(book.id) { mutableLongStateOf(0L) }
        var latestDuration by remember(book.id) { mutableLongStateOf(0L) }
        var leaving by remember(book.id) { mutableStateOf(false) }

        LaunchedEffect(book.id) {
            resumePosition = runCatching {
                graph.data.progress.latest(itemId)?.position?.toLong() ?: 0L
            }.getOrDefault(0L)
            lastSaved = resumePosition
            resumeLoaded = true
        }

        suspend fun persistProgress(position: Long, duration: Long) {
            val now = graph.nowMillis()
            graph.data.progress.save(
                ReadingProgress(
                    itemId = itemId,
                    entryId = book.location,
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

        VideoPlayerScreen(
            title = book.title,
            uri = book.location,
            startPositionMillis = resumePosition,
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
