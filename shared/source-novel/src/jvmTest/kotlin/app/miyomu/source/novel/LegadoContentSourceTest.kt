package app.miyomu.source.novel

import app.miyomu.novel.NovelBlock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LegadoContentSourceTest {

    private val sourceJson = """
        {
          "bookSourceUrl": "https://s.example",
          "bookSourceName": "示例书源",
          "ruleToc": { "chapterList": "ul.chapters li", "chapterName": "a@text", "chapterUrl": "a@href" },
          "ruleContent": { "content": "#content@textNodes" }
        }
    """.trimIndent()

    private val tocPage = """
        <html><body><ul class="chapters">
          <li><a href="/b/1/c1">第一章 起点</a></li>
          <li><a href="/b/1/c2">第二章 转折</a></li>
        </ul></body></html>
    """.trimIndent()

    private fun contentPage(text: String) =
        "<html><body><div id=\"content\"><p>$text</p></div></body></html>"

    private fun store(fakeBodies: MutableMap<String, String>): LegadoContentSource =
        LegadoContentSource(
            source = LegadoBookSource.fromJson(sourceJson),
            book = LegadoOnlineBook(
                name = "测试在线书",
                author = "作者",
                bookUrl = "https://s.example/b/1",
                tocUrl = "https://s.example/b/1/toc",
                coverUrl = null,
                sourceUrl = "https://s.example",
            ),
            fetch = { url -> fakeBodies[url] ?: error("no fixture for $url") },
            js = LegadoQuickJsEngine(),
        )

    @Test
    fun `loads chapters with progress keys and cached toc`() = runTest {
        val bodies = mutableMapOf(
            "https://s.example/b/1/toc" to tocPage,
            "https://s.example/b/1/c1" to contentPage("第一段正文。"),
            "https://s.example/b/1/c2" to contentPage("第二段正文。"),
        )
        val contentSource = store(bodies)

        val first = contentSource.load("book-key", 0, 0)

        assertEquals(2, first.chapterCount)
        assertEquals(0, first.chapterIndex)
        assertEquals("第一章 起点", first.chapterTitle)
        assertEquals("第一段正文。", first.text)
        assertEquals("legado:https://s.example:https://s.example/b/1", first.itemId)
        assertEquals(
            LegadoContentSource.chapterEntryId("https://s.example/b/1", 0),
            first.entryId,
        )
        assertEquals(1, first.blocks.size)
        assertIs<NovelBlock.Text>(first.blocks[0])

        // Second chapter, with a restore offset carried through.
        val second = contentSource.load("book-key", 1, 42)
        assertEquals("第二章 转折", second.chapterTitle)
        assertEquals(42, second.pendingOffset)

        // The TOC page is fetched once; removing its fixture must not break chapter loads.
        bodies.remove("https://s.example/b/1/toc")
        val third = contentSource.load("book-key", 0, 0)
        assertNull(third.error)
        assertEquals("第一段正文。", third.text)
    }

    @Test
    fun `fetch failures surface as error data`() = runTest {
        val contentSource = store(mutableMapOf())

        val data = contentSource.load("book-key", 0, 0)

        assertTrue(data.error != null)
        assertEquals(0, data.chapterCount)
        assertEquals("", data.text)
    }

    @Test
    fun `empty toc surfaces as error data`() = runTest {
        val contentSource = LegadoContentSource(
            source = LegadoBookSource.fromJson(sourceJson),
            book = LegadoOnlineBook("书", null, "https://s.example/b/9", "https://s.example/b/9/toc", null, null),
            fetch = { "<html><body><ul class=\"chapters\"></ul></body></html>" },
            js = LegadoQuickJsEngine(),
        )

        val data = contentSource.load("book-key", 0, 0)

        assertEquals("目录为空", data.error)
    }
}
