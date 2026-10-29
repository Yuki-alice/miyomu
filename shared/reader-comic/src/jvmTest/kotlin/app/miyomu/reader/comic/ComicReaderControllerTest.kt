package app.miyomu.reader.comic

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalPage
import app.miyomu.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ComicReaderControllerTest {

    private val book = LocalBook(
        id = "/library/book",
        title = "Book",
        format = LocalBookFormat.DIRECTORY,
        mediaType = MediaType.COMIC,
        location = "/library/book",
    )

    private val chapters = listOf(
        LocalChapter(
            id = "/library/book/chapter-1",
            title = "Chapter 1",
            bookLocation = book.location,
            location = "/library/book/chapter-1",
            format = LocalBookFormat.DIRECTORY,
        ),
        LocalChapter(
            id = "/library/book/chapter-2",
            title = "Chapter 2",
            bookLocation = book.location,
            location = "/library/book/chapter-2",
            format = LocalBookFormat.DIRECTORY,
        ),
    )

    private val repository = FakeRepository()

    @Test
    fun `open loads chapters and clamps the requested page`() = runBlocking {
        val controller = controller()

        controller.open(book, chapterIndex = 1, pageIndex = 99)

        val state = controller.state.value
        assertEquals(book, state.book)
        assertEquals(2, state.chapters.size)
        assertEquals(1, state.chapterIndex)
        assertEquals(1, state.pageIndex)
        assertEquals(2, state.pages.size)
        assertNull(state.error)
    }

    @Test
    fun `page and mode changes update state`() = runBlocking {
        val controller = controller()
        controller.open(book)

        controller.setPage(1)
        controller.setMode(ComicReadingMode.VERTICAL)

        val state = controller.state.value
        assertEquals(1, state.pageIndex)
        assertEquals(ComicReadingMode.VERTICAL, state.mode)
    }

    @Test
    fun `chapter navigation moves to the next chapter`() = runBlocking {
        val controller = controller()
        controller.open(book)

        controller.nextChapter()

        val state = controller.state.value
        assertEquals(1, state.chapterIndex)
        assertEquals(0, state.pageIndex)
        assertEquals(2, state.pages.size)
    }

    @Test
    fun `empty chapters produce a readable error state`() = runBlocking {
        val controller = ComicReaderController(EmptyRepository(), CoroutineScope(Dispatchers.Unconfined))

        controller.open(book)

        val state = controller.state.value
        assertNotNull(state.error)
        assertEquals(false, state.loading)
    }

    @Test
    fun `pairing keeps the first page alone by default`() {
        val pages = (0 until 5).map { LocalPage(it, "$it.jpg", "/p/$it.jpg") }

        val pairs = pairPages(pages)

        assertEquals(3, pairs.size)
        assertEquals(0, pairs[0].first.index)
        assertNull(pairs[0].second)
        assertEquals(1, pairs[1].first.index)
        assertEquals(2, pairs[1].second?.index)
        assertEquals(3, pairs[2].first.index)
        assertEquals(4, pairs[2].second?.index)
    }

    @Test
    fun `dual page toggles update state and follow the mode`() = runBlocking {
        val controller = controller()
        controller.open(book)

        controller.setDualPage(true)

        val state = controller.state.value
        assertEquals(true, state.dualPage)
        assertEquals(true, state.usesDualPage)

        controller.setMode(ComicReadingMode.WEBTOON)

        val webtoonState = controller.state.value
        assertEquals(true, webtoonState.dualPage)
        assertEquals(false, webtoonState.supportsDualPage)
        assertEquals(false, webtoonState.usesDualPage)
    }

    @Test
    fun `first page alone can be disabled for spreads`() = runBlocking {
        val controller = controller()
        controller.open(book)

        controller.setFirstPageAlone(false)

        assertEquals(false, controller.state.value.firstPageAlone)
    }

    @Test
    fun `spreads cover all pages and map indices in both directions`() {
        val pages = (0 until 8).map { LocalPage(it, "$it.jpg", "/p/$it.jpg") }

        val single = pageSpreads(pages, dualPage = false)
        assertEquals(8, single.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7), single.map { it.startIndex })

        val dual = pageSpreads(pages, dualPage = true, firstPageAlone = true)
        assertEquals(5, dual.size)
        assertEquals(listOf(0, 1, 3, 5, 7), dual.map { it.startIndex })
        assertEquals(2, dual[1].pages.size)
        assertEquals(1, dual.last().pages.size)

        assertEquals(0, spreadIndexForPage(dual, 0))
        assertEquals(1, spreadIndexForPage(dual, 2))
        assertEquals(2, spreadIndexForPage(dual, 4))
        assertEquals(3, spreadIndexForPage(dual, 6))
        assertEquals(4, spreadIndexForPage(dual, 7))
    }

    private fun controller() = ComicReaderController(repository, CoroutineScope(Dispatchers.Unconfined))

    private inner class FakeRepository : LocalComicRepository {
        override suspend fun chapters(book: LocalBook): List<LocalChapter> = chapters

        override suspend fun pages(chapter: LocalChapter): List<LocalPage> = listOf(
            LocalPage(0, "1.jpg", "${chapter.location}/1.jpg"),
            LocalPage(1, "2.jpg", "${chapter.location}/2.jpg"),
        )

        override suspend fun readPage(page: LocalPage): ByteArray = byteArrayOf(page.index.toByte())

        override suspend fun cover(book: LocalBook): ByteArray? = null
    }

    private class EmptyRepository : LocalComicRepository {
        override suspend fun chapters(book: LocalBook): List<LocalChapter> = emptyList()
        override suspend fun pages(chapter: LocalChapter): List<LocalPage> = emptyList()
        override suspend fun readPage(page: LocalPage): ByteArray = ByteArray(0)
        override suspend fun cover(book: LocalBook): ByteArray? = null
    }
}
