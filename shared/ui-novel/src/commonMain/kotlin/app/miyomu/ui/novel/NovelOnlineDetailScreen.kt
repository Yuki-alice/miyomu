package app.miyomu.ui.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.miyomu.model.ReadingProgress
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoContentSource
import app.miyomu.source.novel.LegadoOnlineBook
import app.miyomu.source.novel.LegadoQuickJsEngine
import app.miyomu.source.novel.LegadoSourceReader
import app.miyomu.source.novel.LegadoTocChapter
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private fun sourceFor(graph: MiyomuGraph, book: LegadoOnlineBook): LegadoBookSource =
    graph.novelSourceStore.sources().firstOrNull { it.bookSourceUrl == book.sourceUrl }
        ?: LegadoBookSource(bookSourceUrl = book.sourceUrl)

/**
 * Online book detail: metadata, shelf toggle and the chapter list; chapters open the
 * shared novel reader over [LegadoContentSource]. Progress keys follow the online scheme.
 */
@Composable
fun NovelOnlineDetailScreen(
    graph: MiyomuGraph,
    book: LegadoOnlineBook,) {
    val navigator = LocalMiyomuNavigator.current
        val fetcher = graph.novelFetcher

        val jsEngine = remember { LegadoQuickJsEngine() }
        DisposableEffect(jsEngine) {
            onDispose { jsEngine.close() }
        }
        val reader = remember { LegadoSourceReader(sourceFor(graph, book), jsEngine) }

        var toc by remember { mutableStateOf<List<LegadoTocChapter>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(book.bookUrl) {
            if (fetcher == null) {
                error = "offline"
                return@LaunchedEffect
            }
            runCatching { reader.toc(fetcher(book.tocUrl), book.tocUrl) }
                .onSuccess { toc = it }
                .onFailure { failure -> error = failure.message ?: failure.toString() }
        }

        val itemId = "legado:${book.sourceUrl.orEmpty()}:${book.bookUrl}"
        val progress by produceState<ReadingProgress?>(initialValue = null, itemId) {
            value = runCatching { graph.data.progress.latest(itemId) }.getOrNull()
        }
        // P1-2 缓存全本: prefetches every chapter through the offline-first content source, so
        // the book reads without network afterwards. Progress shows in the title column; a
        // second tap cancels.
        val cacheScope = rememberCoroutineScope()
        var downloadJob by remember { mutableStateOf<Job?>(null) }
        var downloadProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        fun cacheWholeBook() {
            val store = graph.downloadStore
            val chapters = toc
            if (store == null || chapters.isNullOrEmpty()) return
            val running = downloadJob
            if (running != null) {
                running.cancel()
                downloadJob = null
                downloadProgress = null
                return
            }
            val contentSource = LegadoContentSource(
                source = sourceFor(graph, book),
                book = book,
                fetch = { url -> fetcher?.invoke(url).orEmpty() },
                js = jsEngine,
                cache = store,
            )
            downloadJob = cacheScope.launch {
                // Name the entry so the unified download center shows a readable title.
                store.registerNovelBook(itemId, book.name)
                var done = 0
                for (index in chapters.indices) {
                    if (!store.isCached(itemId, chapters[index].url)) {
                        contentSource.load(book.bookUrl, index, 0)
                    }
                    done += 1
                    downloadProgress = done to chapters.size
                }
                downloadProgress = null
                downloadJob = null
            }.also { job -> job.invokeOnCompletion { downloadJob = null } }
        }

        val savedIndex = toc?.indices?.firstOrNull { index ->
            LegadoContentSource.chapterEntryId(book.bookUrl, index) == progress?.entryId
        } ?: -1
        val resumeChapter = savedIndex.coerceAtLeast(0)
        val resumeOffset = if (savedIndex >= 0) progress?.position?.toInt()?.coerceAtLeast(0) ?: 0 else 0

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(book.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            text = downloadProgress?.let { (done, total) -> "缓存中 $done/$total" }
                                ?: book.author.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (downloadProgress != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = ::cacheWholeBook,
                        enabled = graph.downloadStore != null && fetcher != null && !toc.isNullOrEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "缓存全本",
                        )
                    }
                    IconButton(
                        onClick = {
                            if (graph.novelSourceStore.inShelf(book.bookUrl)) {
                                graph.novelSourceStore.removeFromShelf(book.bookUrl)
                            } else {
                                graph.novelSourceStore.addToShelf(book)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (graph.novelSourceStore.inShelf(book.bookUrl)) {
                                Icons.Outlined.Check
                            } else {
                                Icons.Outlined.Add
                            },
                            contentDescription = stringResource(
                                if (graph.novelSourceStore.inShelf(book.bookUrl)) {
                                    MR.strings.novel_in_shelf
                                } else {
                                    MR.strings.novel_add_shelf
                                },
                            ),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!toc.isNullOrEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            navigator.push(
                                book.toReaderKey(resumeChapter, resumeOffset),
                            )
                        },
                        icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null) },
                        text = {
                            Text(
                                text = if (savedIndex >= 0) {
                                    stringResource(MR.strings.action_resume)
                                } else {
                                    stringResource(MR.strings.action_start)
                                },
                            )
                        },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    error != null -> Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                    toc == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    else -> LazyColumn(modifier = Modifier.fillMaxSize().testTag("novel-online-chapters")) {
                        val chapters = toc.orEmpty()
                        items(
                            count = chapters.size,
                            key = { index -> chapters[index].url + index },
                        ) { index ->
                            Text(
                                text = chapters[index].title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigator.push(
                                            book.toReaderKey(index, 0),
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
