package app.miyomu.ui.sources

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
import app.miyomu.contract.EpisodeRecord
import app.miyomu.model.AnimeEpisode
import app.miyomu.model.ComicDetails
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ReadingProgress
import app.miyomu.model.ResolvedVideo
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.*
import app.miyomu.ui.core.resolveEpisodeStream
import eu.kanade.presentation.entries.anime.AnimeScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreenModel
import eu.kanade.tachiyomi.ui.entries.anime.EpisodeList
import eu.kanade.tachiyomi.animesource.AnimeSource as PortedAnimeSource
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryDisplayMode

/** Detail for a search result of a VIDEO source, backed by the ported Aniyomi `AnimeScreen`. */
@Composable
fun SourceAnimeDetailScreen(
    graph: MiyomuGraph,
    sourceId: String,
    sourceName: String,
    summary: app.miyomu.model.ComicSummary,) {
        val navigator = LocalMiyomuNavigator.current
        SourceAnimeDetailContent(
            graph = graph,
            sourceId = sourceId,
            sourceName = sourceName,
            summary = summary,
            onBack = { navigator.pop() },
        )
    }


@Composable
fun SourceAnimeDetailContent(
    graph: MiyomuGraph,
    sourceId: String,
    sourceName: String,
    summary: app.miyomu.model.ComicSummary,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalMiyomuNavigator.current
    val itemId = remember(summary.id) { LibraryItem.idOf(sourceId, summary.id) }

    var details by remember(summary.id) { mutableStateOf<ComicDetails?>(null) }
    var records by remember(summary.id) { mutableStateOf<List<EpisodeRecord>>(emptyList()) }
    var progressByEntry by remember(summary.id) { mutableStateOf<Map<String, ReadingProgress>>(emptyMap()) }
    var favorite by remember(summary.id) { mutableStateOf(false) }
    var error by remember(summary.id) { mutableStateOf<String?>(null) }
    var reloadToken by remember(summary.id) { mutableIntStateOf(0) }
    var selectedIds by remember(summary.id) { mutableStateOf(emptySet<Long>()) }

    fun reloadMarks() {
        scope.launch {
            records = runCatching { graph.data.anime.episodes(itemId) }.getOrDefault(emptyList())
            progressByEntry = runCatching { graph.data.progress.all(itemId) }
                .getOrDefault(emptyList())
                .associateBy { it.entryId }
        }
    }

    LaunchedEffect(summary.id, reloadToken) {
        error = null
        runCatching { graph.sourceRuntime.details(sourceId, summary.id) }
            .onSuccess { loaded ->
                details = loaded
                // Persist the fetched episode list (marks survive refreshes); then read back.
                runCatching {
                    graph.data.anime.replaceEpisodes(
                        itemId,
                        loaded.episodes.map { episode ->
                            EpisodeRecord(
                                key = episode.key,
                                name = episode.name,
                                url = episode.url,
                                episodeNumber = episode.number,
                                sourceOrder = loaded.episodes.indexOf(episode).toLong(),
                            )
                        },
                        fetchedAt = graph.nowMillis(),
                    )
                }
                reloadMarks()
            }
            .onFailure { failure -> error = failure.message ?: failure::class.simpleName }
        favorite = runCatching { graph.data.library.get(itemId) != null }.getOrDefault(false)
    }

    val loadedDetails = details
    if (loadedDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (error == null) {
                CircularProgressIndicator()
            } else {
                androidx.compose.material3.Text(
                    text = error.orEmpty(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                )
            }
        }
        return
    }

    val animeId = sourceAnimeId(sourceId, loadedDetails.id)
    val onlineSource = remember(sourceId, sourceName) {
        object : PortedAnimeSource {
            override val id: Long = sourceId.hashCode().toLong()
            override val name: String = sourceName
            override val lang: String = ""
        }
    }

    // Sites with mirror lines expose duplicates; line 0 is the primary route for now.
    val visibleEpisodes = remember(loadedDetails) {
        val primary = loadedDetails.episodes.filter { it.line == 0 }
        if (primary.isEmpty()) loadedDetails.episodes else primary
    }

    fun play(episode: AnimeEpisode, replace: Boolean = false) {
        scope.launch {
            val resolved = resolveEpisodeStream(graph, sourceId, episode.url, itemId) ?: return@launch
            val stream = resolved.bestStream() ?: return@launch
            val key = OnlineVideoPlayerKey(
                sourceId = sourceId,
                itemId = itemId,
                animeTitle = loadedDetails.title,
                episodes = visibleEpisodes,
                episodeIndex = visibleEpisodes.indexOf(episode).coerceAtLeast(0),
                uri = stream.url,
                headers = resolved.headers,
            )
            if (replace) navigator.replace(key) else navigator.push(key)
        }
    }

    val recordByKey = remember(records) { records.associateBy { it.key } }
    val domainEpisodes = remember(visibleEpisodes, recordByKey, progressByEntry, animeId) {
        visibleEpisodes.mapIndexed { index, episode ->
            episode.toDomainEpisode(
                animeId = animeId,
                index = index,
                record = recordByKey[episode.key],
                progress = progressByEntry[episode.url],
            )
        }
    }
    val items = remember(domainEpisodes, selectedIds) {
        domainEpisodes.map { episode ->
            EpisodeList.Item(
                episode = episode,
                downloadState = AnimeDownload.State.NOT_DOWNLOADED,
                downloadProgress = 0,
                selected = episode.id in selectedIds,
            )
        }
    }
    val anime = remember(loadedDetails, sourceId, favorite) {
        loadedDetails.toAnime(sourceId, favorite, graph.nowMillis())
    }
    val state = remember(anime, items) {
        AnimeScreenModel.State.Success(
            anime = anime,
            source = onlineSource,
            isFromSource = true,
            episodes = items,
            seasons = emptyList(),
        )
    }
    val resumeEpisode = remember(visibleEpisodes, recordByKey, progressByEntry) {
        val latestEntry = progressByEntry.values.maxByOrNull { it.updatedAt }?.entryId
        visibleEpisodes.firstOrNull { it.url == latestEntry && !(recordByKey[it.key]?.seen ?: false) }
            ?: visibleEpisodes.firstOrNull { !(recordByKey[it.key]?.seen ?: false) }
            ?: visibleEpisodes.firstOrNull()
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
            visibleEpisodes.firstOrNull { sourceEpisodeId(it.key) == episode.id }?.let(::play)
        },
        // P1-2 视频按集: START/START_NOW enqueue MP4 episodes; DELETE removes the offline copy
        // and cancels any running task. The queue resolves + streams through the source runtime.
        onDownloadEpisode = { items, action ->
            val queue = graph.downloadQueue ?: return@AnimeScreen
            val targets = items.mapNotNull { item ->
                visibleEpisodes.firstOrNull { sourceEpisodeId(it.key) == item.id }
            }
            targets.forEach { episode ->
                val request = app.miyomu.download.VideoDownloadRequest(
                    sourceId = sourceId,
                    itemId = itemId,
                    episodeUrl = episode.url,
                    episodeTitle = episode.name,
                )
                when (action) {
                    eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction.START,
                    eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction.START_NOW,
                    -> queue.enqueueVideo(request)
                    eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction.DELETE -> {
                        queue.cancelForEpisode(request.itemId, request.episodeUrl)
                        graph.downloadStore?.deleteEpisode(request.itemId, request.episodeUrl)
                    }
                    eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction.CANCEL,
                    eu.kanade.presentation.entries.anime.components.EpisodeDownloadAction.SHOW_QUALITIES,
                    -> Unit
                }
            }
        },
        onAddToLibraryClicked = {
            scope.launch {
                val now = graph.nowMillis()
                if (favorite) {
                    graph.data.library.delete(itemId)
                    graph.data.anime.clear(itemId)
                } else {
                    graph.data.library.upsert(
                        LibraryItem(
                            id = itemId,
                            mediaType = MediaType.VIDEO,
                            sourceId = sourceId,
                            sourceKey = summary.id,
                            title = loadedDetails.title,
                            coverUrl = loadedDetails.coverUrl,
                            author = loadedDetails.tags["author"]?.firstOrNull(),
                            description = loadedDetails.description,
                            status = LibraryStatus.UNKNOWN,
                            addedAt = now,
                            updatedAt = now,
                        ),
                    )
                }
                favorite = !favorite
            }
        },
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = null,
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = { reloadToken++ },
        onContinueWatching = { resumeEpisode?.let(::play) },
        onSearch = { _, _ -> },
        onCoverClicked = {},
        onShareClicked = null,
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        changeAnimeSkipIntro = null,
        onMultiBookmarkClicked = { episodes: List<tachiyomi.domain.items.episode.model.Episode>, bookmarked: Boolean ->
            scope.launch {
                val keys = domainEpisodes.filter { domain -> episodes.any { it.id == domain.id } }
                    .mapNotNull { domain -> visibleEpisodes.firstOrNull { sourceEpisodeId(it.key) == domain.id }?.key }
                graph.data.anime.setBookmarked(itemId, keys, bookmarked)
                reloadMarks()
            }
        },
        onMultiFillermarkClicked = { _, _ -> },
        onMultiMarkAsSeenClicked = { episodes: List<tachiyomi.domain.items.episode.model.Episode>, seen: Boolean ->
            scope.launch {
                val keys = domainEpisodes.filter { domain -> episodes.any { it.id == domain.id } }
                    .mapNotNull { domain -> visibleEpisodes.firstOrNull { sourceEpisodeId(it.key) == domain.id }?.key }
                graph.data.anime.setSeen(itemId, keys, seen)
                reloadMarks()
            }
        },
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
        onContinueWatchingClicked = {},
        onRelatedAnimeClicked = {},
        onRelatedAnimeLongClicked = {},
        relatedAnimeDisplayMode = LibraryDisplayMode.CompactGrid,
    )
}
