package app.miyomu.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalPage
import app.miyomu.local.ReaderPageSource
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.ui.ComicReaderScreen
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/**
 * Reads a chapter straight from an installed source through the shared reader chrome. Chapters
 * and pages are adapted to the local shapes, so no second viewer is needed.
 */
class OnlineReaderScreen(
    private val graph: MiyomuGraph,
    private val sourceId: String,
    private val comicId: String,
    private val title: String,
    private val chapters: List<app.miyomu.model.Chapter>,
    private val chapterIndex: Int,
    private val pageSourceOverride: ReaderPageSource? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val book = remember(sourceId, comicId) {
            LocalBook(
                id = "$sourceId:$comicId",
                title = title,
                format = LocalBookFormat.DIRECTORY,
                mediaType = MediaType.COMIC,
                location = "",
            )
        }
        val pageSource = pageSourceOverride
            ?: remember(sourceId, comicId, chapters) {
                OnlinePageSource(graph.sourceRuntime, sourceId, comicId, chapters)
            }

        ComicReaderScreen(
            book = book,
            initialChapterIndex = chapterIndex,
            initialPageIndex = 0,
            libraryItemId = LibraryItem.idOf(sourceId, comicId),
            pageSource = pageSource,
            data = graph.data,
            deviceId = graph.deviceId,
            nowMillis = graph.nowMillis,
            readerPreferences = graph.readerPreferences,
            onBack = { navigator.pop() },
        )
    }
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
