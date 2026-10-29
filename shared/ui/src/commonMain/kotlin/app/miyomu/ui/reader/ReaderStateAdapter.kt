package app.miyomu.ui.reader

import app.miyomu.reader.comic.ComicReaderUiState
import app.miyomu.reader.comic.ComicReadingMode
import app.miyomu.ui.entries.toDomainChapter
import app.miyomu.ui.library.toManga
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.WebtoonViewer

/**
 * Adapter between Miyomu's reader state and the state the ported reader chrome expects.
 *
 * The chrome (`ReaderAppBars`, `ReaderSettingsDialog`, `ReadingModeSelectDialog`) was written
 * against Aniyomi's `ReaderViewModel.State`; Miyomu's `ComicReaderController` keeps its own,
 * smaller state. Rather than rewriting the chrome, the two are bridged here (ADR-0011).
 */
fun ComicReaderUiState.toReaderState(
    chromeVisible: Boolean,
    bookmarked: Boolean = false,
): ReaderViewModel.State {
    val book = book ?: return ReaderViewModel.State(menuVisible = chromeVisible)

    val currChapter = chapters.getOrNull(chapterIndex)
    val viewerChapters = currChapter?.let {
        ViewerChapters(
            currChapter = ReaderChapter(it.toDomainChapter(book, chapterIndex)),
            prevChapter = chapters.getOrNull(chapterIndex - 1)
                ?.let { prev -> ReaderChapter(prev.toDomainChapter(book, chapterIndex - 1)) },
            nextChapter = chapters.getOrNull(chapterIndex + 1)
                ?.let { next -> ReaderChapter(next.toDomainChapter(book, chapterIndex + 1)) },
        )
    }

    return ReaderViewModel.State(
        manga = book.toManga(),
        viewerChapters = viewerChapters,
        bookmarked = bookmarked,
        currentPage = pageIndex,
        viewer = mode.toViewer(),
        menuVisible = chromeVisible,
    )
}

fun ComicReadingMode.toViewer(): Viewer = when (this) {
    ComicReadingMode.LEFT_TO_RIGHT -> L2RPagerViewer()
    ComicReadingMode.RIGHT_TO_LEFT -> R2LPagerViewer()
    ComicReadingMode.VERTICAL -> VerticalPagerViewer()
    ComicReadingMode.WEBTOON -> WebtoonViewer()
}

fun ComicReadingMode.toReadingMode(): ReadingMode = when (this) {
    ComicReadingMode.LEFT_TO_RIGHT -> ReadingMode.LEFT_TO_RIGHT
    ComicReadingMode.RIGHT_TO_LEFT -> ReadingMode.RIGHT_TO_LEFT
    ComicReadingMode.VERTICAL -> ReadingMode.VERTICAL
    ComicReadingMode.WEBTOON -> ReadingMode.WEBTOON
}

/**
 * Miyomu's viewer has four modes; Aniyomi's `DEFAULT` ("follow the series setting") has no
 * equivalent, and `CONTINUOUS_VERTICAL` maps onto the same webtoon viewer.
 */
fun ReadingMode.toComicReadingMode(): ComicReadingMode? = when (this) {
    ReadingMode.LEFT_TO_RIGHT -> ComicReadingMode.LEFT_TO_RIGHT
    ReadingMode.RIGHT_TO_LEFT -> ComicReadingMode.RIGHT_TO_LEFT
    ReadingMode.VERTICAL -> ComicReadingMode.VERTICAL
    ReadingMode.WEBTOON, ReadingMode.CONTINUOUS_VERTICAL -> ComicReadingMode.WEBTOON
    ReadingMode.DEFAULT -> null
}

/** The chrome shows the book title as the app-bar title and the chapter as its subtitle. */
fun ComicReaderUiState.readerTitles(): Pair<String?, String?> =
    book?.title to currentChapter?.title
