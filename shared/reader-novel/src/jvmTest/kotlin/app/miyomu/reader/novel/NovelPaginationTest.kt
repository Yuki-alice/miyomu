package app.miyomu.reader.novel

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelPaginationTest {

    private fun line(top: Float, bottom: Float, offset: Int) =
        NovelPagination.LineMetric(top, bottom, offset)

    @Test
    fun `groups lines into pages by height budget`() {
        // Five 20px lines (tops 0/20/40/60/80) in a 50px page: 2 + 2 + 1 lines.
        val lines = (0..4).map { line(it * 20f, it * 20f + 18f, it * 10) }

        val pages = NovelPagination.paginate(lines, pageHeight = 50f)

        assertEquals(3, pages.size)
        assertEquals(0, pages[0].startOffset)
        assertEquals(20, pages[0].endOffset) // lines 2..4 start the next page
        assertEquals(20, pages[1].startOffset)
        assertEquals(40, pages[1].endOffset)
        assertEquals(40, pages[2].startOffset)
    }

    @Test
    fun `tall line that alone overflows still opens its own page`() {
        val lines = listOf(
            line(0f, 40f, 0),
            line(40f, 200f, 10), // taller than the 50px budget
            line(200f, 220f, 20),
        )

        val pages = NovelPagination.paginate(lines, pageHeight = 50f)

        assertEquals(3, pages.size)
        assertEquals(0, pages[0].startOffset)
        assertEquals(10, pages[1].startOffset)
        assertEquals(20, pages[2].startOffset)
    }

    @Test
    fun `single line produces a single full chapter page`() {
        val pages = NovelPagination.paginate(listOf(line(0f, 30f, 0)), pageHeight = 50f)

        assertEquals(1, pages.size)
        assertEquals(0, pages[0].startOffset)
        assertTrue(pages[0].contains(0))
        assertTrue(pages[0].contains(Int.MAX_VALUE - 1))
    }

    @Test
    fun `empty chapter produces no pages`() {
        assertTrue(NovelPagination.paginate(emptyList(), pageHeight = 50f).isEmpty())
    }

    @Test
    fun `offset lookup finds the containing page`() {
        val lines = (0..7).map { line(it * 20f, it * 20f + 18f, it * 10) }
        val pages = NovelPagination.paginate(lines, pageHeight = 50f)

        assertEquals(0, NovelPagination.pageIndexForOffset(pages, 0))
        assertEquals(0, NovelPagination.pageIndexForOffset(pages, 19))
        assertEquals(1, NovelPagination.pageIndexForOffset(pages, 20))
        assertEquals(1, NovelPagination.pageIndexForOffset(pages, 39))
        assertEquals(2, NovelPagination.pageIndexForOffset(pages, 40))
        assertEquals(3, NovelPagination.pageIndexForOffset(pages, 60))
        // Past-the-end offsets clamp to the last page.
        assertEquals(pages.lastIndex, NovelPagination.pageIndexForOffset(pages, 999_999))
    }
}
