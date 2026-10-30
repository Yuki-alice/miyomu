package app.miyomu.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.miyomu.contract.ChapterRecord
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalPage
import app.miyomu.local.ReaderPageSource
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.LocalMiyomuNavigator

/**
 * Reads a chapter straight from an installed source through the shared reader chrome. Chapters
 * and pages are adapted to the local shapes, so no second viewer is needed.
 */
@Composable
fun OnlineReaderScreen(
    graph: MiyomuGraph,
    sourceId: String,
    comicId: String,
    title: String,
    chapterIndex: Int,
) {
    val navigator = LocalMiyomuNavigator.current
    val itemId = remember(sourceId, comicId) { LibraryItem.idOf(sourceId, comicId) }

    // Chapters come from the persisted list, so opening a chapter never depends on a live fetch
    // and the destination only has to carry the index.
    val records by produceState<List<ChapterRecord>?>(null, itemId) {
        value = runCatching { graph.data.chapters.chapters(itemId) }.getOrDefault(emptyList())
    }
    val chapters = records?.map { app.miyomu.model.Chapter(id = it.key, title = it.name) }
    if (chapters == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val book = remember(sourceId, comicId) {
        LocalBook(
            id = "$sourceId:$comicId",
            title = title,
            format = LocalBookFormat.DIRECTORY,
            mediaType = MediaType.COMIC,
            location = "",
        )
    }
    // A downloaded chapter wins over the source, matching the detail screen's offline path.
    val offline = remember(itemId, chapters, chapterIndex) {
        val store = graph.downloadStore
        val chapter = chapters.getOrNull(chapterIndex)
        if (store != null && chapter != null) {
            store.pageSource(itemId, chapter.id, chapter.title)
        } else {
            null
        }
    }
    val pageSource = offline
        ?: remember(sourceId, comicId, chapters) {
            OnlinePageSource(graph.sourceRuntime, sourceId, comicId, chapters)
        }

    ComicReaderScreen(
        book = book,
        initialChapterIndex = chapterIndex,
        initialPageIndex = 0,
        libraryItemId = itemId,
        pageSource = pageSource,
        data = graph.data,
        deviceId = graph.deviceId,
        nowMillis = graph.nowMillis,
        readerPreferences = graph.readerPreferences,
        onBack = { navigator.pop() },
    )
}


private class OnlinePageSource(
    private val runtime: SourceRuntimeManager,
    private val sourceId: String,
    private val comicId: String,
    private val sourceChapters: List<app.miyomu.model.Chapter>,
) : ReaderPageSource {

    private val headersByUrl = mutableMapOf<String, Map<String, String>>()

    override suspend fun chapters(book: LocalBook): List<LocalChapter> =
        sourceChapters.mapIndexed { index, chapter ->
            LocalChapter(
                id = chapter.id,
                title = chapter.title,
                bookLocation = book.location,
                location = chapter.id,
                format = LocalBookFormat.DIRECTORY,
                lastModified = 0L,
            )
        }

    override suspend fun pages(chapter: LocalChapter): List<LocalPage> {
        val pages = runtime.pages(sourceId, comicId, chapter.location)
        return pages.mapIndexed { index, page ->
            headersByUrl[page.url] = page.headers
            LocalPage(index = index, name = "第 ${index + 1} 页", location = page.url)
        }
    }

    override suspend fun readPage(page: LocalPage): ByteArray =
        runtime.loadImage(sourceId, page.location, headersByUrl[page.location].orEmpty())
}
