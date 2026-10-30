package app.miyomu.ui.reader

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.LocalMiyomuNavigator

/** Port of Aniyomi's ui/reader/ReaderActivity as a full-screen Voyager Screen. */
@Composable
fun ReaderScreen(
    graph: MiyomuGraph,
    book: LocalBook,
    chapterIndex: Int,
    pageIndex: Int,) {
    val navigator = LocalMiyomuNavigator.current

        ComicReaderScreen(
            book = book,
            initialChapterIndex = chapterIndex,
            initialPageIndex = pageIndex,
            libraryItemId = LibraryItem.idOf("local", book.id),
            pageSource = graph.comics,
            data = graph.data,
            deviceId = graph.deviceId,
            nowMillis = graph.nowMillis,
            readerPreferences = graph.readerPreferences,
            readingStats = graph.readingStats,
            onBack = { navigator.pop() },
        )
    }
