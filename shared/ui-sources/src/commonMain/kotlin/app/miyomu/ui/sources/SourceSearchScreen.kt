package app.miyomu.ui.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.model.ComicSummary
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

/** Search inside one installed source and open its results. */
@Composable
fun SourceSearchScreen(
    graph: MiyomuGraph,
    sourceId: String,
    sourceName: String,
    mediaType: String = "COMIC",) {
        val navigator = LocalMiyomuNavigator.current
        SourceSearchContent(
            graph = graph,
            sourceId = sourceId,
            sourceName = sourceName,
            onBack = { navigator.pop() },
            onOpen = { summary ->
                if (mediaType == "VIDEO") {
                    navigator.push(SourceAnimeDetailKey(sourceId, sourceName, summary.id, summary.title, summary.coverUrl))
                } else {
                    navigator.push(SourceDetailKey(sourceId, sourceName, summary.id, summary.title, summary.coverUrl))
                }
            },
        )
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSearchContent(
    graph: MiyomuGraph,
    sourceId: String,
    sourceName: String,
    onBack: () -> Unit,
    onOpen: (ComicSummary) -> Unit,
) {
    val runtime = graph.sourceRuntime
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ComicSummary>>(emptyList()) }
    var page by remember { mutableIntStateOf(0) }
    var hasNext by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun runSearch(nextPage: Int) {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            runCatching { runtime.search(sourceId, query.trim(), nextPage) }
                .onSuccess { paged ->
                    results = if (nextPage == 1) paged.items else results + paged.items
                    page = nextPage
                    hasNext = paged.hasNext
                }
                .onFailure { failure -> error = failure.message ?: failure::class.simpleName }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sourceName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { runSearch(1) }, enabled = !loading) {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("搜索") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch(1) }),
                trailingIcon = {
                    IconButton(onClick = { runSearch(1) }, enabled = !loading) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    }
                },
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (results.isEmpty() && !loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (error == null) "输入关键词开始搜索" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(results, key = { it.id }) { summary ->
                        ComicCard(summary = summary, onClick = { onOpen(summary) })
                    }
                }
                if (hasNext) {
                    TextButton(
                        onClick = { runSearch(page + 1) },
                        enabled = !loading,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) { Text("加载更多") }
                }
            }
        }
    }
}

@Composable
private fun ComicCard(summary: ComicSummary, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable(onClick = onClick)) {
        Column {
            AsyncImage(
                model = summary.coverUrl,
                contentDescription = summary.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            )
            Text(
                text = summary.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
            summary.author?.takeIf { it.isNotBlank() }?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 6.dp),
                )
            }
        }
    }
}
