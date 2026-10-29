package app.miyomu.ui.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
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
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.entries.anime.AnimeSeasonItem
import eu.kanade.tachiyomi.ui.entries.anime.EpisodeList
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryDisplayMode

/**
 * Detail entry for a local video book, backed by the ported Aniyomi `AnimeScreen` (ADR-0011,
 * Phase B). A folder of videos is a multi-episode anime; a folder of folders is a series whose
 * seasons are pushed onto the same screen.
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
            onPlay = { episode -> navigator.push(VideoPlayerRoute(graph, book, episode)) },
            onOpenSeason = { season -> navigator.push(VideoDetailScreen(graph, season)) },
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
    onPlay: (LocalChapter) -> Unit,
    onOpenSeason: (LocalBook) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }
    var chapters by remember(book.id) { mutableStateOf<List<LocalChapter>?>(null) }
    var seasonBooks by remember(book.id) { mutableStateOf<List<LocalBook>>(emptyList()) }
    var seasonItems by remember(book.id) { mutableStateOf<List<AnimeSeasonItem>>(emptyList()) }
    var progressByEntry by remember(book.id) { mutableStateOf<Map<String, ReadingProgress>>(emptyMap()) }
    var selectedIds by remember(book.id) { mutableStateOf(emptySet<Long>()) }
    var reloadToken by remember(book.id) { mutableIntStateOf(0) }

    LaunchedEffect(book.id, reloadToken) {
        val loadedChapters = runCatching { graph.comics.chapters(book) }.getOrDefault(emptyList())
        val loadedSeasons = runCatching { graph.comics.seasons(book) }.getOrDefault(emptyList())
        val progress = runCatching { graph.data.progress.all(itemId) }.getOrDefault(emptyList())

        chapters = loadedChapters
        seasonBooks = loadedSeasons
        progressByEntry = progress.associateBy { it.entryId }
        seasonItems = loadedSeasons.map { seasonBook ->
            val seasonId = LibraryItem.idOf("local", seasonBook.id)
            val seasonChapters = runCatching { graph.comics.chapters(seasonBook) }.getOrDefault(emptyList())
            val seasonProgress = runCatching { graph.data.progress.all(seasonId) }.getOrDefault(emptyList())
            seasonBook.toSeasonItem(seasonChapters, seasonProgress)
        }
    }

    val loadedChapters = chapters
    if (loadedChapters == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // A mixed folder (direct videos plus season subfolders) stays an episode list.
    val isSeries = loadedChapters.isEmpty() && seasonBooks.isNotEmpty()
    val anime = remember(book, isSeries) {
        book.toAnime(if (isSeries) FetchType.Seasons else FetchType.Episodes)
    }
    val episodes = remember(loadedChapters, progressByEntry, selectedIds) {
        loadedChapters.mapIndexed { index, chapter ->
            EpisodeList.Item(
                episode = chapter.toEpisode(book, progressByEntry[chapter.location], index),
                downloadState = AnimeDownload.State.NOT_DOWNLOADED,
                downloadProgress = 0,
                selected = chapter.episodeId() in selectedIds,
            )
        }
    }
    val state = remember(anime, episodes, seasonItems) {
        AnimeScreenModel.State.Success(
            anime = anime,
            source = LocalVideoSource,
            isFromSource = true,
            episodes = episodes,
            seasons = seasonItems,
        )
    }

    val resumeChapter = remember(loadedChapters, progressByEntry) {
        val latestEntry = progressByEntry.values.maxByOrNull { it.updatedAt }?.entryId
        loadedChapters.firstOrNull { it.location == latestEntry } ?: loadedChapters.firstOrNull()
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
        onEpisodeClicked = { episode, _ ->
            loadedChapters.firstOrNull { it.episodeId() == episode.id }?.let(onPlay)
        },
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
                reloadToken++
            }
        },
        onContinueWatching = { resumeChapter?.let(onPlay) },
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
            selectedIds = if (selected) episodes.map { it.id }.toSet() else emptySet()
        },
        onInvertSelection = {
            selectedIds = episodes.map { it.id }.toSet() - selectedIds
        },
        onSeasonClicked = { season ->
            seasonBooks.firstOrNull { it.animeId() == season.anime.id }?.let(onOpenSeason)
        },
        onContinueWatchingClicked = null,
        onRelatedAnimeClicked = {},
        onRelatedAnimeLongClicked = {},
        relatedAnimeDisplayMode = LibraryDisplayMode.CompactGrid,
    )
}
