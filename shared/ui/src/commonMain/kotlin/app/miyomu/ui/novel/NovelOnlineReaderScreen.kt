package app.miyomu.ui.novel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoContentSource
import app.miyomu.source.novel.LegadoOnlineBook
import app.miyomu.source.novel.LegadoQuickJsEngine
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/** Online variant of the reader: chapter content is fetched from the legado source. */
class NovelOnlineReaderScreen(
    private val graph: MiyomuGraph,
    private val book: LegadoOnlineBook,
    private val chapterIndex: Int,
    private val charOffset: Int,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val fetcher = graph.novelFetcher

        val jsEngine = remember { LegadoQuickJsEngine() }
        DisposableEffect(jsEngine) {
            onDispose { jsEngine.close() }
        }

        val contentSource = remember(book.bookUrl) {
            val source = graph.novelSourceStore.sources()
                .firstOrNull { it.bookSourceUrl == book.sourceUrl }
                ?: LegadoBookSource(bookSourceUrl = book.sourceUrl)
            LegadoContentSource(
                source = source,
                book = book,
                fetch = { url -> fetcher?.invoke(url).orEmpty() },
                js = jsEngine,
            )
        }

        NovelReaderContent(
            bookKey = book.bookUrl,
            bookTitle = book.name,
            initialChapterIndex = chapterIndex,
            initialCharOffset = charOffset,
            contentSource = contentSource,
            bookmarkStore = graph.novelBookmarks,
            data = graph.data,
            deviceId = graph.deviceId,
            nowMillis = graph.nowMillis,
            preferences = graph.novelReaderPreferences,
            onBack = { navigator.pop() },
        )
    }
}
