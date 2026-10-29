package app.miyomu.ui.reader

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.ui.ComicReaderScreen
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/** Port of Aniyomi's ui/reader/ReaderActivity as a full-screen Voyager Screen. */
class ReaderScreen(
    private val graph: MiyomuGraph,
    private val book: LocalBook,
    private val chapterIndex: Int,
    private val pageIndex: Int,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

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
            onBack = { navigator.pop() },
        )
    }
}
