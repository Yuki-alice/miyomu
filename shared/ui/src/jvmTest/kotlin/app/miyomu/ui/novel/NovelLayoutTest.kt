package app.miyomu.ui.novel

import app.miyomu.novel.NovelBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelLayoutTest {

    @Test
    fun `splits paragraphs preserving offsets`() {
        val text = "第一段\n\n第三段\n第四段"
        val paragraphs = splitParagraphs(text)

        // Blank lines produce no piece; offsets point at paragraph starts.
        assertEquals(listOf(0 to "第一段", 5 to "第三段", 9 to "第四段"), paragraphs)
    }

    @Test
    fun `scroll items interleave text lines and images with aligned offsets`() {
        val blocks = listOf(
            NovelBlock.Text(0, "aaaa\nbbbb"),
            NovelBlock.Image(10, byteArrayOf(1), "png", 2, 3),
            NovelBlock.Text(10, "cccc"),
        )

        val items = scrollItems(blocks)

        // The first text block contributes two lines, then the image, then the tail text.
        assertEquals(4, items.size)
        assertEquals(0, items[0].offset)
        assertEquals("aaaa", (items[0] as ScrollItem.TextItem).text)
        assertEquals(5, items[1].offset)
        assertTrue(items[2] is ScrollItem.ImageItem)
        assertEquals(10, items[2].offset)
        assertEquals(10, items[3].offset)
        assertEquals("cccc", (items[3] as ScrollItem.TextItem).text)
    }

    @Test
    fun `scroll item lookup finds containing item`() {
        val items = scrollItems(listOf(NovelBlock.Text(0, "aaaa\nbbbb\ncccc")))

        assertEquals(0, scrollItemIndexForOffset(items, 0))
        assertEquals(0, scrollItemIndexForOffset(items, 4))
        assertEquals(1, scrollItemIndexForOffset(items, 5))
        assertEquals(2, scrollItemIndexForOffset(items, 10))
        assertEquals(2, scrollItemIndexForOffset(items, 999))
        assertEquals(0, scrollItemIndexForOffset(emptyList(), 10))
    }

    @Test
    fun `image piece height follows aspect ratio and clamps to the page`() {
        val wide = NovelBlock.Image(0, ByteArray(1), "png", 100, 50)
        assertEquals(50, imagePieceHeight(wide, widthPx = 100, pageHeightPx = 400))

        val tall = NovelBlock.Image(0, ByteArray(1), "png", 50, 400)
        assertEquals(300, imagePieceHeight(tall, widthPx = 100, pageHeightPx = 300))

        val unknown = NovelBlock.Image(0, ByteArray(1), "img", 0, 0)
        assertEquals(400 * 4 / 3, imagePieceHeight(unknown, widthPx = 400, pageHeightPx = 100_000))
    }
}
