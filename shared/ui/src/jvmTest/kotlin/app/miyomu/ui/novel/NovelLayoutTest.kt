package app.miyomu.ui.novel

import kotlin.test.Test
import kotlin.test.assertEquals

class NovelLayoutTest {

    @Test
    fun `splits paragraphs preserving offsets`() {
        val text = "第一段\n\n第三段\n第四段"
        val paragraphs = splitParagraphs(text)

        // Blank lines produce no piece; offsets point at paragraph starts.
        assertEquals(listOf(0 to "第一段", 5 to "第三段", 9 to "第四段"), paragraphs)
    }

    @Test
    fun `paragraph lookup finds containing paragraph`() {
        val text = "aaaa\nbbbb\ncccc"
        val paragraphs = splitParagraphs(text)

        assertEquals(0, paragraphIndexForOffset(paragraphs, 0))
        assertEquals(0, paragraphIndexForOffset(paragraphs, 4))
        assertEquals(1, paragraphIndexForOffset(paragraphs, 5))
        assertEquals(2, paragraphIndexForOffset(paragraphs, 10))
        assertEquals(2, paragraphIndexForOffset(paragraphs, 999))
        assertEquals(0, paragraphIndexForOffset(emptyList(), 10))
    }
}
