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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoExploreCategory
import app.miyomu.source.novel.LegadoSearchBook
import app.miyomu.source.novel.exploreLegadoSource
import app.miyomu.source.novel.toOnlineBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.*
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/** Discovery categories and their book lists for one imported legado source. */
@Composable
fun NovelExploreScreen(
    graph: MiyomuGraph,
    source: LegadoBookSource,) {
    val navigator = LocalMiyomuNavigator.current
        val fetcher = graph.novelFetcher
        val categories = remember(source) { source.exploreCategories() }
        var selected by remember { mutableStateOf<LegadoExploreCategory?>(null) }
        var books by remember { mutableStateOf<List<LegadoSearchBook>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(false) }

        LaunchedEffect(selected) {
            val category = selected ?: return@LaunchedEffect
            if (fetcher == null) return@LaunchedEffect
            loading = true
            error = null
            books = null
            runCatching { exploreLegadoSource(source, fetcher, category) }
                .onSuccess { books = it }
                .onFailure { failure -> error = failure.message ?: failure.toString() }
            loading = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (selected != null) selected = null else navigator.pop()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = selected?.title ?: source.bookSourceName ?: source.bookSourceUrl.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }

            when {
                fetcher == null -> Text(
                    text = stringResource(MR.strings.novel_explore_empty),
                    modifier = Modifier.padding(16.dp),
                )
                selected == null && categories.isEmpty() -> Text(
                    text = stringResource(MR.strings.novel_explore_empty),
                    modifier = Modifier.padding(16.dp),
                )
                selected == null -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().testTag("novel-explore-categories"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(categories.size, key = { index -> categories[index].title + categories[index].url }) { index ->
                        val category = categories[index]
                        Text(
                            text = category.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = category }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
                loading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                error != null -> Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                books == null -> Unit
                books!!.isEmpty() -> Text(
                    text = stringResource(MR.strings.novel_search_no_results),
                    modifier = Modifier.padding(16.dp),
                )
                else -> LazyColumn(modifier = Modifier.weight(1f).testTag("novel-explore-results")) {
                    items(books!!.size, key = { index -> books!![index].bookUrl.orEmpty() + index }) { index ->
                        val book = books!![index]
                        SearchBookRow(
                            graph = graph,
                            book = book,
                            sourceUrl = source.bookSourceUrl,
                            onClick = {
                                navigator.push(book.toOnlineBook(source.bookSourceUrl).toDetailKey())
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
