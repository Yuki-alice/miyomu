package app.miyomu.ui.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoOnlineBook
import app.miyomu.source.novel.LegadoQuickJsEngine
import app.miyomu.source.novel.LegadoSourceReader
import app.miyomu.source.novel.LegadoSearchBook
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Search page for one imported legado source; results open the online detail screen. */
class NovelSourceSearchScreen(
    private val graph: MiyomuGraph,
    private val source: LegadoBookSource,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val fetcher = graph.novelFetcher

        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<LegadoSearchBook>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var searching by remember { mutableStateOf(false) }
        var searchTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(query, searchTrigger) {
            if (query.isBlank() || fetcher == null) return@LaunchedEffect
            delay(300)
            searching = true
            error = null
            results = null
            runCatching {
                val reader = LegadoSourceReader(source, LegadoQuickJsEngine())
                val url = reader.searchUrl(query) ?: throw IllegalStateException("searchUrl")
                reader.search(fetcher(url), url)
            }.onSuccess { results = it }
                .onFailure { failure -> error = failure.message ?: failure.toString() }
            searching = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = source.bookSourceName ?: source.bookSourceUrl.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(MR.strings.novel_search_online)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("novel-online-search"),
                )
                IconButton(
                    onClick = { searchTrigger++ },
                    enabled = query.isNotBlank() && fetcher != null,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(MR.strings.action_search))
                }
            }

            when {
                searching -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                error != null -> Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                results == null -> Text(
                    text = stringResource(MR.strings.novel_search_online),
                    modifier = Modifier.padding(16.dp),
                )
                results!!.isEmpty() -> Text(
                    text = stringResource(MR.strings.novel_search_no_results),
                    modifier = Modifier.padding(16.dp),
                )
                else -> LazyColumn(modifier = Modifier.weight(1f).testTag("novel-online-results")) {
                    items(
                        count = results!!.size,
                        key = { index -> results!![index].bookUrl.orEmpty() + index },
                    ) { index ->
                        val book = results!![index]
                        SearchBookRow(
                            graph = graph,
                            book = book,
                            sourceUrl = source.bookSourceUrl,
                            onClick = {
                                navigator.push(
                                    NovelOnlineDetailScreen(
                                        graph = graph,
                                        book = LegadoOnlineBook(
                                            name = book.name,
                                            author = book.author,
                                            bookUrl = book.bookUrl.orEmpty(),
                                            tocUrl = book.bookUrl.orEmpty(),
                                            coverUrl = book.coverUrl,
                                            sourceUrl = source.bookSourceUrl,
                                        ),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SearchBookRow(
    graph: MiyomuGraph,
    book: LegadoSearchBook,
    sourceUrl: String?,
    onClick: () -> Unit,
) {
    val shelf by graph.novelSourceStore.shelf.collectAsState()
    val onShelf = shelf.any { it.bookUrl == book.bookUrl }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(book.name, maxLines = 1)
            val secondary = listOfNotNull(book.author, book.latestChapter).joinToString(" · ")
            if (secondary.isNotEmpty()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        IconButton(
            onClick = {
                if (onShelf) {
                    graph.novelSourceStore.removeFromShelf(book.bookUrl.orEmpty())
                } else {
                    graph.novelSourceStore.addToShelf(
                        LegadoOnlineBook(
                            name = book.name,
                            author = book.author,
                            bookUrl = book.bookUrl.orEmpty(),
                            tocUrl = book.bookUrl.orEmpty(),
                            coverUrl = book.coverUrl,
                            sourceUrl = sourceUrl,
                        ),
                    )
                }
            },
        ) {
            Icon(
                imageVector = if (onShelf) Icons.Outlined.Check else Icons.Outlined.Add,
                contentDescription = stringResource(
                    if (onShelf) MR.strings.novel_in_shelf else MR.strings.novel_add_shelf,
                ),
            )
        }
    }
}
