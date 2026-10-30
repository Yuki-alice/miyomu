package app.miyomu.ui.novel

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
import app.miyomu.model.LibraryItem
import app.miyomu.model.ReadingProgress
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.ParsedNovel
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.share.bookMimeType
import app.miyomu.ui.share.rememberBookSharer
import app.miyomu.ui.track.TrackBindingDialog
import app.miyomu.ui.navigation.*
import app.miyomu.ui.library.mangaId
import app.miyomu.ui.library.toManga
import eu.kanade.presentation.entries.manga.MangaScreen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import eu.kanade.tachiyomi.ui.entries.manga.ChapterList
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreenModel
import tachiyomi.domain.items.chapter.model.Chapter
import tachiyomi.domain.source.manga.model.StubMangaSource

/**
 * Detail entry pushed on the root navigator, backed by the ported Aniyomi `MangaScreen`.
 * Chapters come from the parsed novel (TXT heading scan or EPUB spine); progress keys are
 * the positional chapter ids from [LocalNovelRepository.chapterId].
 */
@Composable
fun NovelDetailScreen(
    graph: MiyomuGraph,
    book: LocalBook,) {        val navigator = LocalMiyomuNavigator.current

        NovelDetailContent(
            graph = graph,
            book = book,
            onBack = { navigator.pop() },
            onRead = { chapterIndex, charOffset ->
                navigator.push(NovelReaderKey(book.id, chapterIndex, charOffset))
            },
        )
    }


/** Novel books sit under the same reserved local source as comics; only the label differs. */
private val LocalNovelSource = StubMangaSource(id = 0L, lang = "local", name = "本地文本")

@Composable
fun NovelDetailContent(
    graph: MiyomuGraph,
    book: LocalBook,
    onBack: () -> Unit,
    onRead: (chapterIndex: Int, charOffset: Int) -> Unit,
) {
    val itemId = remember(book.id) { LibraryItem.idOf("local", book.id) }
    var parsed by remember(book.id) { mutableStateOf<ParsedNovel?>(null) }
    var latestProgress by remember(book.id) { mutableStateOf<ReadingProgress?>(null) }
    var refreshToken by remember(book.id) { mutableIntStateOf(0) }
    var selectedIds by remember(book.id) { mutableStateOf(emptySet<Long>()) }
    var showTrackDialog by remember(book.id) { mutableStateOf(false) }

    LaunchedEffect(book.id, refreshToken) {
        parsed = runCatching { graph.novels.parse(book) }.getOrNull()
        latestProgress = runCatching { graph.data.progress.latest(itemId) }.getOrNull()
    }

    val loaded = parsed
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val chapterCount = loaded.chapters.size
    val savedChapterIndex = remember(loaded, latestProgress) {
        loaded.chapters.indices.firstOrNull { index ->
            LocalNovelRepository.chapterId(book, index) == latestProgress?.entryId
        } ?: -1
    }
    val resumeChapter = savedChapterIndex.coerceAtLeast(0)
    val resumeOffset = if (savedChapterIndex >= 0) {
        latestProgress?.position?.toInt()?.coerceAtLeast(0) ?: 0
    } else {
        0
    }

    val items = remember(loaded, savedChapterIndex, selectedIds) {
        loaded.chapters.mapIndexed { index, novelChapter ->
            val chapter = novelChapterToDomain(book, index, novelChapter.title)
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
        source = LocalNovelSource,
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
            val index = items.indexOfFirst { it.id == chapter.id }
            if (index >= 0) onRead(index, 0)
        },
        onDownloadChapter = null,
        onAddToLibraryClicked = {},
        onWebViewClicked = null,
        onWebViewLongClicked = null,
        onTrackingClicked = { showTrackDialog = true },
        onTagSearch = {},
        onFilterButtonClicked = {},
        onRefresh = { refreshToken++ },
        onContinueReading = { onRead(resumeChapter, resumeOffset) },
        onSearch = { _, _ -> },
        onCoverClicked = {},
        onShareClicked = rememberBookSharer()?.let { sharer ->
            { sharer(book.location, bookMimeType(book.format)) }
        },
        onDownloadActionClicked = null,
        onEditCategoryClicked = null,
        onEditFetchIntervalClicked = null,
        onMigrateClicked = null,
        // Read/bookmark flags have no storage yet (same slice state as the manga detail).
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

    if (showTrackDialog) {
        // P1-9 逐书绑定: chapters before the resume point count as read for the tracker push.
        TrackBindingDialog(
            graph = graph,
            itemId = itemId,
            initialTitle = book.title,
            currentProgress = savedChapterIndex.coerceAtLeast(0),
            onDismiss = { showTrackDialog = false },
        )
    }
}

/** Domain id for the chapter list UI; progress keys use [LocalNovelRepository.chapterId]. */
internal fun novelChapterDomainId(book: LocalBook, index: Int): Long =
    LocalNovelRepository.chapterId(book, index).hashCode().toLong() and 0xFFFFFFFFL

internal fun novelChapterToDomain(book: LocalBook, index: Int, title: String?): Chapter = Chapter(
    id = novelChapterDomainId(book, index),
    mangaId = book.mangaId(),
    read = false,
    bookmark = false,
    lastPageRead = 0L,
    dateFetch = book.lastModified,
    sourceOrder = index.toLong(),
    url = "${book.location}#$index",
    name = title ?: (index + 1).toString(),
    dateUpload = book.lastModified,
    chapterNumber = (index + 1).toDouble(),
    scanlator = null,
    lastModifiedAt = book.lastModified,
    version = 1L,
)
