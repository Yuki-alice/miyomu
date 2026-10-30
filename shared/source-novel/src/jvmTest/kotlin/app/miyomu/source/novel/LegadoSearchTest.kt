package app.miyomu.source.novel

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The shared search entry point behind both the per-source screen and the global search.
 */
class LegadoSearchTest {

    private val sourceJson = """
        {
          "bookSourceUrl": "https://novel.example",
          "bookSourceName": "示例书源",
          "searchUrl": "https://novel.example/search?q={{key}}&p={{page}}",
          "ruleSearch": {
            "bookList": "div.book-item",
            "name": "h3 a@text",
            "author": ".author@text",
            "bookUrl": "h3 a@href",
            "coverUrl": "img@src"
          }
        }
    """.trimIndent()

    private val page = """
        <html><body>
          <div class="book-item">
            <h3><a href="/book/1">斗破苍穹</a></h3>
            <span class="author">天蚕土豆</span>
            <img src="/cover/1.jpg"/>
          </div>
        </body></html>
    """.trimIndent()

    @Test
    fun `builds the search url, fetches once and maps hits`() = runTest {
        var requested: String? = null
        val fetcher: suspend (String) -> String = { url ->
            requested = url
            page
        }

        val hits = searchLegadoSource(LegadoBookSource.fromJson(sourceJson), fetcher, "斗破 苍穹")

        val url = requested ?: error("fetcher was not called")
        assertTrue(url.startsWith("https://novel.example/search?"), url)
        assertTrue(url.contains("p=1"), url)
        // The keyword must be encoded, not spliced in raw.
        assertTrue(!url.contains("苍穹"), url)

        assertEquals(1, hits.size)
        assertEquals("斗破苍穹", hits.single().name)
        assertEquals("天蚕土豆", hits.single().author)
    }

    @Test
    fun `search hit maps onto the shelf model`() = runTest {
        val hit = searchLegadoSource(LegadoBookSource.fromJson(sourceJson), { page }, "斗破").single()

        val online = hit.toOnlineBook("https://novel.example")

        assertEquals(hit.name, online.name)
        assertEquals(hit.author, online.author)
        assertEquals("https://novel.example/book/1", online.bookUrl)
        // The TOC URL is only known after the detail load, so it starts as the book URL.
        assertEquals(online.bookUrl, online.tocUrl)
        assertEquals("https://novel.example", online.sourceUrl)
    }

    @Test
    fun `source without a search url fails loudly`() = runTest {
        val source = LegadoBookSource.fromJson(
            """{"bookSourceUrl":"https://x.example","bookSourceName":"无搜索","ruleSearch":{"bookList":"div"}}""",
        )

        assertFailsWith<IllegalStateException> {
            searchLegadoSource(source, { page }, "q")
        }
    }
}
