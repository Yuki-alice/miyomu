package app.miyomu.ui.entries

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalBook
import app.miyomu.ui.BookDetailScreen
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.reader.ReaderScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/** Port of Aniyomi's ui/entries/manga/MangaScreen: detail entry pushed on the root navigator. */
class MangaDetailScreen(
    private val graph: MiyomuGraph,
    private val book: LocalBook,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        BookDetailScreen(
            book = book,
            comics = graph.comics,
            data = graph.data,
            onBack = { navigator.pop() },
            onRead = { chapterIndex, pageIndex ->
                navigator.push(ReaderScreen(graph, book, chapterIndex, pageIndex))
            },
        )
    }
}
