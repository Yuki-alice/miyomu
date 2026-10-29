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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.reader.OnlineReaderScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.entries.manga.MangaScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.entries.manga.ChapterList
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreenModel

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
                navigator.push(
                    OnlineReaderScreen(
                        graph = graph,
                        sourceId = sourceId,
                        comicId = summary.id,
                        title = summary.title,
                        chapters = chapters,
                        chapterIndex = index,
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
    var details by remember(summary.id) { mutableStateOf<ComicDetails?>(null) }
    var error by remember(summary.id) { mutableStateOf<String?>(null) }
    var reloadToken by remember(summary.id) { mutableIntStateOf(0) }
    var selectedIds by remember(summary.id) { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(summary.id, reloadToken) {
        details = null
        error = null
        runCatching { graph.sourceRuntime.details(sourceId, summary.id) }
            .onSuccess { details = it }
            .onFailure { failure -> error = failure.message ?: failure::class.simpleName }
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
    val manga = remember(loaded, sourceId) { loaded.toManga(sourceId, now) }
    val sourceChapters = loaded.chapters
    val domainChapters = remember(loaded, manga.id) {
        sourceChapters.mapIndexed { index, chapter -> chapter.toDomainChapter(manga.id, index) }
    }
    val items = remember(domainChapters, selectedIds) {
        domainChapters.map { chapter ->
            ChapterList.Item(
                chapter = chapter,
                downloadState = MangaDownload.State.NOT_DOWNLOADED,
                downloadProgress = 0,
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
        onDownloadChapter = null,
        onAddToLibraryClicked = {},
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = {},
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = { reloadToken++ },
        onContinueReading = {
            if (sourceChapters.isNotEmpty()) onRead(sourceChapters, 0)
        },
        onSearch = { _, _ -> },
        onCoverClicked = {},
        onShareClicked = null,
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        // Online read/bookmark flags need a Room chapter table; only selection moves for now.
        onMultiBookmarkClicked = { _, _ -> },
        onMultiMarkAsReadClicked = { _, _ -> },
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
