package app.miyomu.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.MiyomuData
import app.miyomu.data.createMiyomuData
import app.miyomu.model.Chapter
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.LibraryItem
import app.miyomu.model.PageRef
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.source.ManagedComicSource
import app.miyomu.source.runtime.loadJsComicSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.skia.Image as SkiaImage
import java.io.File

private const val FixtureBaseUrl = "http://127.0.0.1:18080"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Miyomu M1 · Source runner",
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                SourceDemo()
            }
        }
    }
}

@Composable
private fun SourceDemo() {
    var runtime by remember { mutableStateOf<DemoRuntime?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        var data: MiyomuData? = null
        try {
            data = createMiyomuData(
                JvmMiyomuDatabaseFactory(
                    File(System.getProperty("java.io.tmpdir"), "miyomu-demo.db").absolutePath,
                ),
            )
            runtime = DemoRuntime(loadDemoSource(), data)
        } catch (failure: CancellationException) {
            data?.close()
            throw failure
        } catch (failure: Exception) {
            data?.close()
            error = failure.message ?: failure.toString()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runtime?.let {
                it.source.close()
                it.data.close()
            }
        }
    }

    val current = runtime
    when {
        error != null -> Message(
            title = "Source failed to load",
            detail = error + "\n\nStart the fixture server:\n" +
                "python3 miyomu/tools/fixture_server.py --port 18080",
        )
        current == null -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading demo source…")
        }
        else -> SourceRunner(current.source, current.data)
    }
}

private data class DemoRuntime(
    val source: ManagedComicSource,
    val data: MiyomuData,
)

private suspend fun loadDemoSource(): ManagedComicSource {
    val script = checkNotNull(
        object {}.javaClass.getResourceAsStream("/sources/demo_source.js"),
    ) { "demo source resource missing" }
        .bufferedReader()
        .use { it.readText() }

    val scriptWithBaseUrl = buildString {
        append("globalThis.__BASE_URL = ")
        append(Json.encodeToString(FixtureBaseUrl))
        append(";\n")
        append(script)
    }

    val dataDir = Path(File(System.getProperty("java.io.tmpdir"), "miyomu-demo-source").absolutePath)
    return loadJsComicSource(
        script = scriptWithBaseUrl,
        dataDir = dataDir,
        expectedKey = "miyomu_demo",
    )
}

@Composable
private fun SourceRunner(source: ManagedComicSource, data: MiyomuData) {
    val scope = rememberCoroutineScope()
    val runner = remember(source, data, scope) { SourceRunnerState(source, data, scope) }
    val state = runner.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("Miyomu M1 · ${source.descriptor.name}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "HTML source read path · ${source.descriptor.version}",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = runner::setQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Search") },
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { runner.search() },
                enabled = !state.busy,
            ) {
                Text(if (state.busy) "Working…" else "Search")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(state.status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))

        val details = state.details
        if (details == null) {
            ResultList(
                results = state.results,
                onOpen = runner::open,
                modifier = Modifier.weight(1f),
            )
        } else {
            DetailPane(
                details = details,
                pages = state.pages,
                image = state.image,
                busy = state.busy,
                onBack = runner::back,
                onChapter = runner::openChapter,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ResultList(
    results: List<ComicSummary>,
    onOpen: (ComicSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) {
        Text("No results yet.", modifier = modifier)
        return
    }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results, key = { it.id }) { comic ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(comic) },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(comic.title, style = MaterialTheme.typography.titleMedium)
                    comic.author?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPane(
    details: ComicDetails,
    pages: List<PageRef>,
    image: ByteArray?,
    busy: Boolean,
    onBack: () -> Unit,
    onChapter: (Chapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(Modifier.width(12.dp))
            Text(details.title, style = MaterialTheme.typography.titleLarge)
        }
        details.description?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        Text("Chapters", style = MaterialTheme.typography.titleMedium)
        if (details.chapters.isEmpty()) {
            Text("No chapters.", style = MaterialTheme.typography.bodySmall)
        }
        details.chapters.forEach { chapter ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(enabled = !busy) { onChapter(chapter) },
            ) {
                Text(
                    text = chapter.title,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        if (pages.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Pages (${pages.size})", style = MaterialTheme.typography.titleMedium)
            if (image != null) {
                val bitmap = remember(image) {
                    runCatching { SkiaImage.makeFromEncoded(image).toComposeImageBitmap() }.getOrNull()
                }
                bitmap?.let {
                    Spacer(Modifier.height(8.dp))
                    Image(
                        bitmap = it,
                        contentDescription = "First page",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            pages.forEach { page ->
                Text(
                    text = page.url,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun Message(title: String, detail: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class SourceUiState(
    val query: String = "demo",
    val status: String = "Ready. Start the local fixture server first.",
    val busy: Boolean = false,
    val results: List<ComicSummary> = emptyList(),
    val details: ComicDetails? = null,
    val pages: List<PageRef> = emptyList(),
    val image: ByteArray? = null,
)

private class SourceRunnerState(
    private val source: ManagedComicSource,
    private val data: MiyomuData,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(SourceUiState())
        private set

    fun setQuery(value: String) {
        state = state.copy(query = value)
    }

    fun search() {
        if (state.busy) return
        scope.launch {
            state = state.copy(busy = true, status = "Searching…", details = null, pages = emptyList(), image = null)
            try {
                val result = source.search(state.query)
                state = state.copy(
                    busy = false,
                    status = "${result.items.size} result(s)",
                    results = result.items,
                )
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                state = state.copy(busy = false, status = "Search failed: ${failure.message}")
            }
        }
    }

    fun open(comic: ComicSummary) {
        if (state.busy) return
        scope.launch {
            state = state.copy(busy = true, status = "Loading details…")
            try {
                val details = source.details(comic.id)
                val libraryCount = try {
                    data.library.upsert(
                        LibraryItem.from(source.descriptor, comic, System.currentTimeMillis()),
                    )
                    data.library.observeLibrary().first().size
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    null
                }
                state = state.copy(
                    busy = false,
                    status = "${details.chapters.size} chapter(s)" +
                        (libraryCount?.let { " · library $it" } ?: ""),
                    details = details,
                    pages = emptyList(),
                    image = null,
                )
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                state = state.copy(busy = false, status = "Details failed: ${failure.message}")
            }
        }
    }

    fun openChapter(chapter: Chapter) {
        val details = state.details ?: return
        if (state.busy) return
        scope.launch {
            state = state.copy(busy = true, status = "Loading pages…")
            try {
                val pages = source.pages(details.id, chapter.id)
                val image = pages.firstOrNull()?.let { page ->
                    try {
                        source.loadImage(page.url, page.headers)
                    } catch (failure: CancellationException) {
                        throw failure
                    } catch (_: Exception) {
                        null
                    }
                }
                val saved = try {
                    val now = System.currentTimeMillis()
                    val itemId = LibraryItem.idOf(source.descriptor.id, details.id)
                    data.progress.save(
                        ReadingProgress(
                            itemId = itemId,
                            entryId = chapter.id,
                            kind = ProgressKind.PAGE,
                            position = 0.0,
                            updatedAt = now,
                            deviceId = "desktop",
                        ),
                    )
                    data.library.markViewed(itemId, now)
                    true
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    false
                }
                state = state.copy(
                    busy = false,
                    status = "${pages.size} page(s)" + if (saved) " · progress saved" else "",
                    pages = pages,
                    image = image,
                )
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                state = state.copy(busy = false, status = "Pages failed: ${failure.message}")
            }
        }
    }

    fun back() {
        state = state.copy(details = null, pages = emptyList(), image = null, status = "Ready.")
    }
}
