package app.miyomu.ui.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
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
import app.miyomu.contract.ChapterRecord
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.model.ReadingProgress
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.track.TrackBindingDialog
import app.miyomu.ui.share.bookMimeType
import app.miyomu.ui.share.rememberBookSharer
import app.miyomu.ui.navigation.*
import app.miyomu.ui.library.CategoryEditDialog
import app.miyomu.ui.library.mangaId
import app.miyomu.ui.library.toManga
import kotlinx.coroutines.launch
import eu.kanade.presentation.entries.manga.MangaScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.entries.manga.ChapterList
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreenModel
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.source.manga.model.StubMangaSource

/**
 * Detail entry pushed on the root navigator, backed by the ported Aniyomi `MangaScreen`
 * (ADR-0011, A2-4).
 */
@Composable
fun MangaDetailScreen(
    graph: MiyomuGraph,
    book: LocalBook,) {        val navigator = LocalMiyomuNavigator.current

        MangaDetailContent(
            graph = graph,
            book = book,
            onBack = { navigator.pop() },
            onRead = { chapterIndex, pageIndex ->
                navigator.push(MangaReaderKey(book.id, chapterIndex, pageIndex))
            },
        )
    }


/**
 * Stands in for Aniyomi's `MangaSource`: local books belong to the reserved source id 0 and have
 * nothing to fetch, so every remote call would throw. The name is what the detail header shows.
 */
private val LocalMangaSource = StubMangaSource(id = 0L, lang = "local", name = "本地文件")

@Composable
fun MangaDetailContent(
    graph: MiyomuGraph,
    book: LocalBook,
    onBack: () -> Unit,
    onRead: (chapterIndex: Int, pageIndex: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }
    var chapters by remember(book.id) { mutableStateOf<List<LocalChapter>?>(null) }
    var records by remember(book.id) { mutableStateOf<List<ChapterRecord>>(emptyList()) }
    var latestProgress by remember(book.id) { mutableStateOf<ReadingProgress?>(null) }
    var refreshToken by remember(book.id) { mutableIntStateOf(0) }
    var selectedIds by remember(book.id) { mutableStateOf(emptySet<Long>()) }
    var showCategoryDialog by remember(book.id) { mutableStateOf(false) }
    var showTrackDialog by remember(book.id) { mutableStateOf(false) }
    var showCoverPicker by remember(book.id) { mutableStateOf(false) }
    val categories by graph.data.categories.observeCategories().collectAsState(initial = emptyList())
    var memberOf by remember(book.id) { mutableStateOf<Set<Long>>(emptySet()) }

    fun reloadMarks() {
        scope.launch {
            records = runCatching { graph.data.chapters.chapters(itemId) }.getOrDefault(emptyList())
            latestProgress = runCatching { graph.data.progress.latest(itemId) }.getOrNull()
            memberOf = runCatching { graph.data.categories.categoryIdsForItem(itemId).toSet() }.getOrDefault(emptySet())
        }
    }

    LaunchedEffect(book.id, refreshToken) {
        chapters = runCatching { graph.comics.chapters(book) }.getOrDefault(emptyList())
        // Persist the chapter list (marks survive rescans), then read back.
        val loaded = chapters.orEmpty()
        if (loaded.isNotEmpty()) {
            runCatching {
                graph.data.chapters.replaceChapters(
                    itemId,
                    loaded.mapIndexed { index, chapter ->
                        ChapterRecord(
                            key = chapter.id,
                            name = chapter.title,
                            url = chapter.location,
                            chapterNumber = (index + 1).toDouble(),
                            sourceOrder = index.toLong(),
                        )
                    },
                    fetchedAt = graph.nowMillis(),
                )
            }
        }
        reloadMarks()
    }

    val loadedChapters = chapters
    if (loadedChapters == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val savedChapterIndex = remember(loadedChapters, latestProgress) {
        loadedChapters.indexOfFirst { it.id == latestProgress?.entryId }
    }
    val resumeChapter = savedChapterIndex.coerceAtLeast(0)
    val resumePage = if (savedChapterIndex >= 0) {
        latestProgress?.position?.toInt()?.coerceAtLeast(0) ?: 0
    } else {
        0
    }

    val recordByKey = remember(records) { records.associateBy { it.key } }
    val items = remember(loadedChapters, recordByKey, savedChapterIndex, selectedIds) {
        loadedChapters.mapIndexed { index, local ->
            val chapter = local.toDomainChapter(book, index)
            val record = recordByKey[local.id]
            ChapterList.Item(
                chapter = chapter.copy(
                    read = record?.read ?: (savedChapterIndex >= 0 && index < savedChapterIndex),
                    bookmark = record?.bookmark ?: false,
                ),
                downloadState = MangaDownload.State.NOT_DOWNLOADED,
                downloadProgress = 0,
                selected = chapter.id in selectedIds,
            )
        }
    }

    val state = MangaScreenModel.State.Success(
        manga = book.toManga(),
        source = LocalMangaSource,
        isFromSource = true,
        chapters = items,
        availableScanlators = emptySet(),
        excludedScanlators = emptySet(),
    )

    // P1-9 逐书绑定: local read-chapter count is the progress pushed to the tracker.
    val readChapterCount = remember(records) { records.count { it.read } }

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
            val index = loadedChapters.indexOfFirst { it.domainId() == chapter.id }
            if (index >= 0) onRead(index, 0)
        },
        // Downloads arrive in Phase C; the chapter action menu hides the entry entirely.
        onDownloadChapter = null,
        onAddToLibraryClicked = {
            scope.launch {
                val stored = graph.data.library.get(itemId)
                if (stored != null) {
                    graph.data.library.delete(itemId)
                } else {
                    graph.data.library.upsert(
                        LibraryItem(
                            id = itemId,
                            mediaType = MediaType.COMIC,
                            sourceId = "local",
                            sourceKey = book.id,
                            title = book.title,
                            addedAt = graph.nowMillis(),
                            updatedAt = graph.nowMillis(),
                        ),
                    )
                }
                refreshToken++
            }
        },
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = { showTrackDialog = true },
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = { refreshToken++ },
        onContinueReading = { onRead(resumeChapter, resumePage) },
        onSearch = { _, _ -> },
        onCoverClicked = { showCoverPicker = true },
        onShareClicked = rememberBookSharer()?.let { sharer ->
            { sharer(book.location, bookMimeType(book.format)) }
        },
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        onMultiBookmarkClicked = { selected, bookmarked ->
            scope.launch {
                val keys = loadedChapters.filter { local -> selected.any { it.id == local.domainId() } }
                    .map { it.id }
                graph.data.chapters.setBookmarked(itemId, keys, bookmarked)
                reloadMarks()
            }
        },
        onMultiMarkAsReadClicked = { selected, read ->
            scope.launch {
                val keys = loadedChapters.filter { local -> selected.any { it.id == local.domainId() } }
                    .map { it.id }
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
            val all = items.map { it.id }.toSet()
            selectedIds = all - selectedIds
        },
    )

    if (showCoverPicker) {
        CoverCandidatePickerDialog(
            graph = graph,
            book = book,
            onDismiss = { showCoverPicker = false },
        )
    }
    if (showTrackDialog) {
        TrackBindingDialog(
            graph = graph,
            itemId = itemId,
            initialTitle = book.title,
            currentProgress = readChapterCount,
            onDismiss = { showTrackDialog = false },
        )
    }
}

/** Stable chapter id derived from the on-disk location. */
internal fun LocalChapter.domainId(): Long = id.hashCode().toLong() and 0xFFFFFFFFL

/**
 * Local chapter -> Aniyomi chapter. Ids are hashed from the on-disk location so they stay stable
 * across rescans, which matters because the reader keys saved progress on the same string.
 */
fun LocalChapter.toDomainChapter(book: LocalBook, index: Int): Chapter = Chapter(
    id = domainId(),
    mangaId = book.mangaId(),
    read = false,
    bookmark = false,
    lastPageRead = 0L,
    dateFetch = lastModified,
    sourceOrder = index.toLong(),
    url = location,
    name = title,
    dateUpload = lastModified,
    chapterNumber = (index + 1).toDouble(),
    scanlator = null,
    lastModifiedAt = lastModified,
    version = 1L,
)
