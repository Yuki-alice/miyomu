package app.miyomu.download

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalPage
import app.miyomu.local.ReaderPageSource
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.MediaType
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.ResolvedVideo
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.manager.SourceRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadQueueTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        scope.coroutineContext.cancelChildren()
    }

    @Test
    fun downloadsPagesAndMarksComplete() = runBlocking {
        val runtime = FakeSourceRuntime(pageCount = 3)
        val store = FileDownloadStore(tempDir)
        val queue = FileDownloadQueue(runtime, store, scope)
        try {
            queue.enqueue(request())
            withTimeout(15_000) {
                queue.state.first { state -> state.tasks.any { it.status == DownloadStatus.DONE } }
            }
            assertEquals(3, store.pageCount("item-1", "ch-1"))
            assertTrue(store.isDownloaded("item-1", "ch-1"))
            assertEquals(listOf<Byte>(1, 0, 0, 0), store.readPage("item-1", "ch-1", 0).toList())

            val source = store.pageSource("item-1", "ch-1", "第一章")!!
            val chapters = source.chapters(dummyBook())
            val pages = source.pages(chapters.single())
            assertEquals(3, pages.size)
            assertEquals(listOf<Byte>(2, 0, 0, 0), source.readPage(pages[1]).toList())
        } finally {
            queue.close()
        }
    }

    @Test
    fun resumesSkippingExistingPages() = runBlocking {
        val store = FileDownloadStore(tempDir)
        // Pre-write page 0: the queue must skip it and still fetch 1..2.
        store.writePage("item-1", "ch-1", 0, byteArrayOf(9, 9))
        val runtime = FakeSourceRuntime(pageCount = 3)
        val queue = FileDownloadQueue(runtime, store, scope)
        try {
            queue.enqueue(request())
            withTimeout(15_000) {
                queue.state.first { state -> state.tasks.any { it.status == DownloadStatus.DONE } }
            }
            assertEquals(listOf<Byte>(9, 9), store.readPage("item-1", "ch-1", 0).toList())
            assertEquals(2, runtime.fetchedUrls.size)
            assertEquals("https://example.test/2.jpg", runtime.fetchedUrls.first())
        } finally {
            queue.close()
        }
    }

    @Test
    fun reportsFailuresWithoutTouchingTheMarker() = runBlocking {
        val runtime = FakeSourceRuntime(pageCount = 2, failOnPage = 1)
        val store = FileDownloadStore(tempDir)
        val queue = FileDownloadQueue(runtime, store, scope)
        try {
            queue.enqueue(request())
            withTimeout(15_000) {
                queue.state.first { state -> state.tasks.any { it.status == DownloadStatus.FAILED } }
            }
            assertEquals(false, store.isDownloaded("item-1", "ch-1"))
            val task = queue.state.value.tasks.single()
            assertTrue(task.error.orEmpty().contains("boom"))
        } finally {
            queue.close()
        }
    }

    @Test
    fun enqueuingADownloadedChapterIsInstantDone() = runBlocking {
        val store = FileDownloadStore(tempDir)
        store.writePage("item-1", "ch-1", 0, byteArrayOf(1))
        store.markComplete("item-1", "ch-1", 1)
        val runtime = FakeSourceRuntime(pageCount = 1)
        val queue = FileDownloadQueue(runtime, store, scope)
        try {
            queue.enqueue(request())
            withTimeout(15_000) {
                queue.state.first { state -> state.tasks.any { it.status == DownloadStatus.DONE } }
            }
            assertEquals(0, runtime.fetchedUrls.size)
        } finally {
            queue.close()
        }
    }

    private fun request() = DownloadChapterRequest(
        sourceId = "html_fixture",
        itemId = "item-1",
        comicId = "comic-1",
        chapterKey = "ch-1",
        chapterTitle = "第一章",
    )
}

private fun dummyBook(): LocalBook = LocalBook(
    id = "item-1",
    title = "Demo",
    format = LocalBookFormat.DIRECTORY,
    mediaType = MediaType.COMIC,
    location = "item-1",
)

/** Serves `pageCount` fake pages; page bytes encode their index; can fail a page on demand. */
private class FakeSourceRuntime(
    private val pageCount: Int,
    private val failOnPage: Int = -1,
) : SourceRuntimeManager {
    val fetchedUrls = mutableListOf<String>()

    private val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(SourceRuntimeState())
    override val state: StateFlow<SourceRuntimeState> = stateFlow

    override suspend fun refresh() = Unit

    override suspend fun search(sourceId: String, query: String, page: Int): Paged<ComicSummary> =
        Paged(emptyList(), hasNext = false)

    override suspend fun details(sourceId: String, comicId: String): ComicDetails =
        ComicDetails(id = comicId, title = comicId)

    override suspend fun pages(sourceId: String, comicId: String, chapterId: String): List<PageRef> =
        (1..pageCount).map { PageRef(url = "https://example.test/$it.jpg") }

    override suspend fun resolveVideo(sourceId: String, episodeUrl: String): ResolvedVideo =
        ResolvedVideo()

    override suspend fun loadImage(sourceId: String, url: String, headers: Map<String, String>): ByteArray {
        val index = url.substringAfterLast("/").substringBefore(".").toInt()
        if (index == failOnPage) throw IllegalStateException("boom on page $index")
        fetchedUrls += url
        delay(10)
        return byteArrayOf(index.toByte(), 0, 0, 0)
    }

    override fun close() = Unit
}
