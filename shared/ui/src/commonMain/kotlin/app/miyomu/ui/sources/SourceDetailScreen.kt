package app.miyomu.ui.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.data.ChapterRecord
import app.miyomu.download.DownloadStatus
import app.miyomu.download.DownloadQueueState
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.LibraryItem
import app.miyomu.model.LibraryStatus
import app.miyomu.model.MediaType
import app.miyomu.model.ReadingProgress
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.reader.OnlineReaderScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.entries.manga.MangaScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.presentation.entries.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.entries.manga.ChapterList
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreenModel
import kotlinx.coroutines.launch

/** Detail for a search result, rendered by the ported Aniyomi `MangaScreen`. */
class SourceDetailScreen(
    private val graph: MiyomuGraph,
    private val sourceId: String,
    private val sourceName: String,
    private val summary: ComicSummary,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        SourceDetailContent(
            graph = graph,
            sourceId = sourceId,
            sourceName = sourceName,
            summary = summary,
            onBack = { navigator.pop() },
            onRead = { chapters, index ->
                val store = graph.downloadStore
                val chapterKey = chapters.getOrNull(index)?.id
                val offline = if (store != null && chapterKey != null) {
                    store.pageSource(
                        LibraryItem.idOf(sourceId, summary.id),
                        chapterKey,
                        chapters.getOrNull(index)?.title ?: summary.title,
                    )
                } else {
                    null
                }
                navigator.push(
                    OnlineReaderScreen(
                        graph = graph,
                        sourceId = sourceId,
                        comicId = summary.id,
                        title = summary.title,
                        chapters = chapters,
                        chapterIndex = index,
                        pageSourceOverride = offline,
                    ),
                )
            },
        )
    }
}

@Composable
fun SourceDetailContent(
    graph: MiyomuGraph,
    sourceId: String,
    sourceName: String,
    summary: ComicSummary,
    onBack: () -> Unit,
    onRead: (chapters: List<app.miyomu.model.Chapter>, chapterIndex: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val itemId = remember(summary.id) { LibraryItem.idOf(sourceId, summary.id) }

    var details by remember(summary.id) { mutableStateOf<ComicDetails?>(null) }
    var records by remember(summary.id) { mutableStateOf<List<ChapterRecord>>(emptyList()) }
    var progressByEntry by remember(summary.id) { mutableStateOf<Map<String, ReadingProgress>>(emptyMap()) }
    var favorite by remember(summary.id) { mutableStateOf(false) }
    var error by remember(summary.id) { mutableStateOf<String?>(null) }
    var reloadToken by remember(summary.id) { mutableIntStateOf(0) }
    var selectedIds by remember(summary.id) { mutableStateOf(emptySet<Long>()) }

    fun reloadMarks() {
        scope.launch {
            records = runCatching { graph.data.chapters.chapters(itemId) }.getOrDefault(emptyList())
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
                // Persist the fetched chapter list; read/bookmark marks survive refreshes.
                runCatching {
                    graph.data.chapters.replaceChapters(
                        itemId,
                        loaded.chapters.mapIndexed { index, chapter ->
                            ChapterRecord(
                                key = chapter.id,
                                name = chapter.title,
                                url = chapter.id,
                                sourceOrder = index.toLong(),
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

    val loaded = details
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (error == null) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = { reloadToken++ }) { Text("重试") }
                }
            }
        }
        return
    }

    val now = graph.nowMillis()
    val manga = remember(loaded, sourceId, favorite) { loaded.toManga(sourceId, now).copy(favorite = favorite) }
    val sourceChapters = loaded.chapters
    val recordByKey = remember(records) { records.associateBy { it.key } }
    val domainChapters = remember(loaded, manga.id, recordByKey) {
        sourceChapters.mapIndexed { index, chapter ->
            val record = recordByKey[chapter.id]
            chapter.toDomainChapter(manga.id, index).copy(
                read = record?.read ?: false,
                bookmark = record?.bookmark ?: false,
            )
        }
    }

    fun downloadStateFor(chapterKey: String, state: DownloadQueueState): MangaDownload.State {
        val store = graph.downloadStore
        if (store != null && store.isDownloaded(itemId, chapterKey)) return MangaDownload.State.DOWNLOADED
        val task = state.tasks.firstOrNull { it.request.itemId == itemId && it.request.chapterKey == chapterKey }
        return when (task?.status) {
            DownloadStatus.RUNNING -> MangaDownload.State.DOWNLOADING
            DownloadStatus.QUEUED -> MangaDownload.State.QUEUE
            DownloadStatus.FAILED -> MangaDownload.State.ERROR
            else -> MangaDownload.State.NOT_DOWNLOADED
        }
    }

    fun downloadProgressFor(chapterKey: String, state: DownloadQueueState): Int =
        state.tasks.firstOrNull { it.request.itemId == itemId && it.request.chapterKey == chapterKey }
            ?.takeIf { it.status == DownloadStatus.RUNNING && it.pagesTotal > 0 }
            ?.let { it.pagesDone * 100 / it.pagesTotal }
            ?: 0

    val queueState by graph.downloadQueue?.state
        ?.collectAsState()
        ?: remember { mutableStateOf(DownloadQueueState()) }
    val items = remember(domainChapters, selectedIds, queueState, favorite) {
        domainChapters.map { chapter ->
            ChapterList.Item(
                chapter = chapter,
                downloadState = downloadStateFor(chapter.url, queueState),
                downloadProgress = downloadProgressFor(chapter.url, queueState),
                selected = chapter.id in selectedIds,
            )
        }
    }
    val state = remember(manga, items) {
        MangaScreenModel.State.Success(
            manga = manga,
            source = OnlineMangaSource(id = sourceId.hashCode().toLong(), name = sourceName),
            isFromSource = true,
            chapters = items,
            availableScanlators = sourceChapters.mapNotNull { it.group }.toSet(),
            excludedScanlators = emptySet(),
        )
    }
    val resumeIndex = remember(sourceChapters, progressByEntry) {
        val latestEntry = progressByEntry.values.maxByOrNull { it.updatedAt }?.entryId
        sourceChapters.indexOfFirst { it.id == latestEntry }.takeIf { it >= 0 } ?: 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val swipeEndAction = remember { graph.libraryPreferences.swipeChapterEndAction().get() }
    val swipeStartAction = remember { graph.libraryPreferences.swipeChapterStartAction().get() }

    MangaScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        nextUpdate = null,
        isTabletUi = isTabletUi(),
        chapterSwipeStartAction = swipeStartAction,
        chapterSwipeEndAction = swipeEndAction,
        navigateUp = onBack,
        onChapterClicked = { chapter ->
            val index = domainChapters.indexOfFirst { it.id == chapter.id }
            if (index >= 0) onRead(sourceChapters, index)
        },
        onDownloadChapter = { chapters, action ->
            val queue = graph.downloadQueue ?: return@MangaScreen
            when (action) {
                ChapterDownloadAction.START -> chapters.forEach { item ->
                    val model = domainChapters.firstOrNull { it.id == item.id } ?: return@forEach
                    queue.enqueue(
                        app.miyomu.download.DownloadChapterRequest(
                            sourceId = sourceId,
                            itemId = itemId,
                            comicId = summary.id,
                            chapterKey = model.url,
                            chapterTitle = model.name,
                        ),
                    )
                }
                ChapterDownloadAction.DELETE -> {
                    val store = graph.downloadStore ?: return@MangaScreen
                    chapters.forEach { item ->
                        domainChapters.firstOrNull { it.id == item.id }?.let { store.deleteChapter(itemId, it.url) }
                    }
                }
                else -> Unit
            }
        },
        onAddToLibraryClicked = {
            scope.launch {
                val nowMillis = graph.nowMillis()
                if (favorite) {
                    graph.data.library.delete(itemId)
                    graph.data.chapters.clear(itemId)
                } else {
                    graph.data.library.upsert(
                        LibraryItem(
                            id = itemId,
                            mediaType = MediaType.COMIC,
                            sourceId = sourceId,
                            sourceKey = summary.id,
                            title = loaded.title,
                            coverUrl = loaded.coverUrl,
                            author = loaded.tags["author"]?.firstOrNull(),
                            description = loaded.description,
                            status = LibraryStatus.UNKNOWN,
                            addedAt = nowMillis,
                            updatedAt = nowMillis,
                        ),
                    )
                }
                favorite = !favorite
            }
        },
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = {},
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = { reloadToken++ },
        onContinueReading = {
            if (sourceChapters.isNotEmpty()) onRead(sourceChapters, resumeIndex)
        },
        onSearch = { _, _ -> },
        onCoverClicked = {},
        onShareClicked = null,
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        onMultiBookmarkClicked = { chapters, bookmarked ->
            scope.launch {
                val keys = domainChapters.filter { domain -> chapters.any { it.id == domain.id } }
                    .map { it.url }
                graph.data.chapters.setBookmarked(itemId, keys, bookmarked)
                reloadMarks()
            }
        },
        onMultiMarkAsReadClicked = { chapters, read ->
            scope.launch {
                val keys = domainChapters.filter { domain -> chapters.any { it.id == domain.id } }
                    .map { it.url }
                graph.data.chapters.setRead(itemId, keys, read)
                reloadMarks()
            }
        },
        onMarkPreviousAsReadClicked = {},
        onMultiDeleteClicked = {},
        onChapterSwipe = { _, _ -> },
        onChapterSelected = { item, selected, _, _ ->
            selectedIds = if (selected) selectedIds + item.id else selectedIds - item.id
        },
        onAllChapterSelected = { selected ->
            selectedIds = if (selected) items.map { it.id }.toSet() else emptySet()
        },
        onInvertSelection = {
            selectedIds = items.map { it.id }.toSet() - selectedIds
        },
    )
}
