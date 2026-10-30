package app.miyomu.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.model.ComicSummary
import app.miyomu.source.manager.SourceRuntimeInfo
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoSearchBook
import app.miyomu.source.novel.searchLegadoSource
import app.miyomu.source.novel.toOnlineBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.novel.NovelOnlineDetailScreen
import app.miyomu.ui.novel.NovelSourceSearchScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.SearchToolbar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * Global search across every loaded source (Aniyomi's Browse > global search): one query fans
 * out concurrently and each source streams its own result card row.
 *
 * Both source families take part: installed JS sources (comic + anime) and imported legado
 * novel sources, so one query covers all three media (docs/12 §7).
 */
class GlobalSearchRoute(
    private val graph: MiyomuGraph,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val runtime = graph.sourceRuntime
        val sources by runtime.state.collectAsState()
        val novelSources by graph.novelSourceStore.sources.collectAsState()
        val novelFetcher = graph.novelFetcher

        var query by rememberSaveable { mutableStateOf<String?>(null) }
        var submitted by rememberSaveable { mutableStateOf("") }
        val results = remember { mutableStateMapOf<String, SourceResult>() }
        val novelResults = remember { mutableStateMapOf<String, NovelResult>() }

        fun runSearch(term: String) {
            if (term.isNullOrBlank()) return
            submitted = term
            results.clear()
            novelResults.clear()
            sources.sources.forEach { info ->
                results[info.id] = SourceResult.Loading
                scope.launch {
                    val outcome = runCatching { runtime.search(info.id, term, 1) }
                    results[info.id] = outcome.fold(
                        onSuccess = { SourceResult.Success(it.items) },
                        onFailure = { SourceResult.Error(it.message ?: it::class.simpleName.orEmpty()) },
                    )
                }
            }
            if (novelFetcher != null) {
                novelSources.forEach { novelSource ->
                    val key = novelKey(novelSource)
                    novelResults[key] = NovelResult.Loading
                    scope.launch {
                        val outcome = runCatching { searchLegadoSource(novelSource, novelFetcher, term) }
                        novelResults[key] = outcome.fold(
                            onSuccess = { NovelResult.Success(it) },
                            onFailure = { NovelResult.Error(it.message ?: it::class.simpleName.orEmpty()) },
                        )
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("全局搜索") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // This screen exists to search, so keep the field visible instead of starting
                // in SearchToolbar's collapsed (icon-only) state.
                SearchToolbar(
                    searchQuery = query ?: "",
                    onChangeSearchQuery = { query = it },
                    navigateUp = null,
                    placeholderText = "在所有源中搜索（漫画 / 动画 / 小说）",
                    onSearch = { term -> runSearch(term.trim()) },
                )

                if (submitted.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "输入关键词，同时搜索所有已安装的源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        sources.sources.forEach { info ->
                            val result = results[info.id] ?: SourceResult.Loading
                            item(key = info.id) {
                                GlobalSourceSection(
                                    info = info,
                                    result = result,
                                    onOpenSource = {
                                        navigator.push(
                                            SourceSearchScreen(graph, info.id, info.name, info.mediaType),
                                        )
                                    },
                                    onOpenItem = { summary ->
                                        if (info.mediaType == "VIDEO") {
                                            navigator.push(SourceAnimeDetailScreen(graph, info.id, info.name, summary))
                                        } else {
                                            navigator.push(SourceDetailScreen(graph, info.id, info.name, summary))
                                        }
                                    },
                                )
                            }
                        }

                        if (novelFetcher != null) {
                            items(novelSources, key = { novelKey(it) }) { novelSource ->
                                val key = novelKey(novelSource)
                                GlobalNovelSection(
                                    title = novelSource.bookSourceName ?: novelSource.bookSourceUrl.orEmpty(),
                                    result = novelResults[key] ?: NovelResult.Loading,
                                    onOpenSource = {
                                        navigator.push(NovelSourceSearchScreen(graph, novelSource))
                                    },
                                    onOpenItem = { book ->
                                        navigator.push(
                                            NovelOnlineDetailScreen(
                                                graph = graph,
                                                book = book.toOnlineBook(novelSource.bookSourceUrl),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun novelKey(source: LegadoBookSource): String =
    "novel:" + (source.bookSourceUrl ?: source.bookSourceName ?: "unnamed")

private sealed interface SourceResult {
    data object Loading : SourceResult
    data class Success(val items: List<ComicSummary>) : SourceResult
    data class Error(val message: String) : SourceResult
}

private sealed interface NovelResult {
    data object Loading : NovelResult
    data class Success(val items: List<LegadoSearchBook>) : NovelResult
    data class Error(val message: String) : NovelResult
}

@Composable
private fun GlobalSourceSection(
    info: SourceRuntimeInfo,
    result: SourceResult,
    onOpenSource: () -> Unit,
    onOpenItem: (ComicSummary) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSource).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (info.mediaType == "VIDEO") {
                Text(
                    text = "anime",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (result) {
            SourceResult.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp))
            }
            is SourceResult.Error -> Text(
                text = result.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            is SourceResult.Success -> {
                if (result.items.isEmpty()) {
                    Text(
                        text = "没有结果",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(result.items.take(10), key = { _, item -> item.id }) { _, summary ->
                            GlobalResultCard(summary = summary, onClick = { onOpenItem(summary) })
                        }
                    }
                }
                if (result.items.size >= 5) {
                    TextButton(onClick = onOpenSource, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("在 ${info.name} 中查看更多")
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalNovelSection(
    title: String,
    result: NovelResult,
    onOpenSource: () -> Unit,
    onOpenItem: (LegadoSearchBook) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSource).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "小说",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when (result) {
            NovelResult.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp))
            }
            is NovelResult.Error -> Text(
                text = result.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            is NovelResult.Success -> {
                if (result.items.isEmpty()) {
                    Text(
                        text = "没有结果",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(result.items.take(10)) { book ->
                            GlobalNovelCard(book = book, onClick = { onOpenItem(book) })
                        }
                    }
                }
                if (result.items.size >= 5) {
                    TextButton(onClick = onOpenSource, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("在 $title 中查看更多")
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalNovelCard(book: LegadoSearchBook, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).width(120.dp),
            )
            book.author?.takeIf { it.isNotBlank() }?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp).width(120.dp),
                )
            }
        }
    }
}

@Composable
private fun GlobalResultCard(summary: ComicSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            AsyncImage(
                model = summary.coverUrl,
                contentDescription = summary.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(120.dp).aspectRatio(2f / 3f),
            )
            Text(
                text = summary.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp).width(120.dp),
            )
        }
    }
}
