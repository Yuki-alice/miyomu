package app.miyomu.ui.novel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoContentSource
import app.miyomu.source.novel.LegadoOnlineBook
import app.miyomu.source.novel.LegadoQuickJsEngine
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import app.miyomu.ui.novel.tts.rememberNovelTts

/** Online variant of the reader: chapter content is fetched from the legado source. */
@Composable
fun NovelOnlineReaderScreen(
    graph: MiyomuGraph,
    book: LegadoOnlineBook,
    chapterIndex: Int,
    charOffset: Int,) {
    val navigator = LocalMiyomuNavigator.current
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
                // Offline-first (P1-2): cached chapters serve without network; every fetch
                // refreshes the cache, so previously read / prefetched chapters work offline.
                cache = graph.downloadStore,
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
            readingStats = graph.readingStats,
            tts = rememberNovelTts(),
            highlightStore = graph.novelHighlights,
            onBack = { navigator.pop() },
        )
    }
