package app.miyomu.ui.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.model.ReadingProgress
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.player.VideoPlayerRoute
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.entries.anime.AnimeScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.entries.anime.EpisodeList
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryDisplayMode

/**
 * Detail entry for a local video book, backed by the ported Aniyomi `AnimeScreen` (ADR-0011,
 * Phase B). A video file maps to a single-episode anime until the scanner learns to group
 * folders into seasons.
 */
class VideoDetailScreen(
    private val graph: MiyomuGraph,
    private val book: LocalBook,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        VideoDetailContent(
            graph = graph,
            book = book,
            onBack = { navigator.pop() },
            onPlay = { navigator.push(VideoPlayerRoute(graph, book)) },
        )
    }
}

/** Local video belongs to the reserved source id 0 and has nothing remote to configure. */
private val LocalVideoSource = object : AnimeSource {
    override val id: Long = 0L
    override val name: String = "本地文件"
    override val lang: String = "local"
}

@Composable
fun VideoDetailContent(
    graph: MiyomuGraph,
    book: LocalBook,
    onBack: () -> Unit,
    onPlay: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }
    var latestProgress by remember(book.id) { mutableStateOf<ReadingProgress?>(null) }
    var progressLoaded by remember(book.id) { mutableStateOf(false) }
    var selectedIds by remember(book.id) { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(book.id) {
        latestProgress = runCatching { graph.data.progress.latest(itemId) }.getOrNull()
        progressLoaded = true
    }

    if (!progressLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val anime = remember(book) { book.toAnime() }
    val episode = remember(book, latestProgress) { book.toEpisode(latestProgress) }
    val items = remember(episode, selectedIds) {
        listOf(
            EpisodeList.Item(
                episode = episode,
                downloadState = AnimeDownload.State.NOT_DOWNLOADED,
                downloadProgress = 0,
                selected = episode.id in selectedIds,
            ),
        )
    }
    val state = remember(anime, items) {
        AnimeScreenModel.State.Success(
            anime = anime,
            source = LocalVideoSource,
            isFromSource = true,
            episodes = items,
            seasons = emptyList(),
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val swipeEndAction = remember { graph.libraryPreferences.swipeEpisodeEndAction().get() }
    val swipeStartAction = remember { graph.libraryPreferences.swipeEpisodeStartAction().get() }

    AnimeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        nextUpdate = null,
        isTabletUi = isTabletUi(),
        episodeSwipeStartAction = swipeStartAction,
        episodeSwipeEndAction = swipeEndAction,
        showNextEpisodeAirTime = false,
        alwaysUseExternalPlayer = false,
        navigateUp = onBack,
        onEpisodeClicked = { _, _ -> onPlay() },
        // Downloads, trackers and source settings arrive in later phases; null hides the entry.
        onDownloadEpisode = null,
        onAddToLibraryClicked = {},
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = null,
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = {
            scope.launch {
                runCatching { graph.library.refresh(graph.root) }
                latestProgress = runCatching { graph.data.progress.latest(itemId) }.getOrNull()
            }
        },
        onContinueWatching = onPlay,
        onSearch = { _, _ -> },
        onCoverClicked = {},
        onShareClicked = null,
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        changeAnimeSkipIntro = null,
        // Seen/bookmark flags have no per-episode storage yet, so these only move the selection.
        onMultiBookmarkClicked = { _, _ -> },
        onMultiFillermarkClicked = { _, _ -> },
        onMultiMarkAsSeenClicked = { _, _ -> },
        onMarkPreviousAsSeenClicked = {},
        onMultiDeleteClicked = {},
        onEpisodeSwipe = { _, _ -> },
        onEpisodeSelected = { item, selected, _, _ ->
            selectedIds = if (selected) selectedIds + item.id else selectedIds - item.id
        },
        onAllEpisodeSelected = { selected ->
            selectedIds = if (selected) items.map { it.id }.toSet() else emptySet()
        },
        onInvertSelection = {
            selectedIds = items.map { it.id }.toSet() - selectedIds
        },
        onSeasonClicked = {},
        onContinueWatchingClicked = null,
        onRelatedAnimeClicked = {},
        onRelatedAnimeLongClicked = {},
        relatedAnimeDisplayMode = LibraryDisplayMode.CompactGrid,
    )
}
