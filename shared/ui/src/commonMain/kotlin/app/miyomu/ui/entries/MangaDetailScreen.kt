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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.model.LibraryItem
import app.miyomu.model.ReadingProgress
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.library.mangaId
import app.miyomu.ui.library.toManga
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
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
class MangaDetailScreen(
    private val graph: MiyomuGraph,
    private val book: LocalBook,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        MangaDetailContent(
            graph = graph,
            book = book,
            onBack = { navigator.pop() },
            onRead = { chapterIndex, pageIndex ->
                navigator.push(app.miyomu.ui.reader.ReaderScreen(graph, book, chapterIndex, pageIndex))
            },
        )
    }
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
    val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }
    var chapters by remember(book.id) { mutableStateOf<List<LocalChapter>?>(null) }
    var latestProgress by remember(book.id) { mutableStateOf<ReadingProgress?>(null) }
    var refreshToken by remember(book.id) { mutableIntStateOf(0) }
    var selectedIds by remember(book.id) { mutableStateOf(emptySet<Long>()) }

    LaunchedEffect(book.id, refreshToken) {
        chapters = runCatching { graph.comics.chapters(book) }.getOrDefault(emptyList())
        latestProgress = runCatching { graph.data.progress.latest(itemId) }.getOrNull()
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

    val items = remember(loadedChapters, savedChapterIndex, selectedIds) {
        loadedChapters.mapIndexed { index, local ->
            val chapter = local.toDomainChapter(book, index)
            ChapterList.Item(
                // A chapter counts as read once the reader has moved past it.
                chapter = chapter.copy(read = savedChapterIndex >= 0 && index < savedChapterIndex),
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
        onAddToLibraryClicked = {},
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = {},
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = { refreshToken++ },
        onContinueReading = { onRead(resumeChapter, resumePage) },
        onSearch = { _, _ -> },
        onCoverClicked = {},
        onShareClicked = null,
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        // Read/bookmark flags have no storage yet: the local library has no chapter table, so
        // these actions only move the selection state in this slice.
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
            val all = items.map { it.id }.toSet()
            selectedIds = all - selectedIds
        },
    )
}

/** Stable chapter id derived from the on-disk location. */
internal fun LocalChapter.domainId(): Long = id.hashCode().toLong() and 0xFFFFFFFFL

/**
 * Local chapter -> Aniyomi chapter. Ids are hashed from the on-disk location so they stay stable
 * across rescans, which matters because the reader keys saved progress on the same string.
 */
internal fun LocalChapter.toDomainChapter(book: LocalBook, index: Int): Chapter = Chapter(
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
