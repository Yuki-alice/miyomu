package app.miyomu.source.novel

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegadoRuleEvaluatorTest {

    private val evaluator = LegadoRuleEvaluator(LegadoQuickJsEngine())

    private val html = parseLegadoHtml(
        """
        <html><body>
          <div class="book-item">
            <h3><a href="/book/1">斗破苍穹</a></h3>
            <span class="author">天蚕土豆</span>
          </div>
          <div class="book-item">
            <h3><a href="/book/2">凡人修仙传</a></h3>
            <span class="author">忘语</span>
          </div>
        </body></html>
        """.trimIndent(),
    )

    @Test
    fun `css selector with attr and text extractors`() = runTest {
        assertEquals("斗破苍穹", evaluator.string("div.book-item h3 a@text", html, "https://s.example/"))
        assertEquals("/book/1", evaluator.string("div.book-item h3 a@href", html, "https://s.example/"))
        assertEquals("天蚕土豆", evaluator.string(".book-item .author@text", html, "https://s.example/"))
        assertNull(evaluator.string("div.missing@text", html, "https://s.example/"))
    }

    @Test
    fun `fallback picks first non-empty alternative`() = runTest {
        assertEquals(
            "斗破苍穹",
            evaluator.string("does-not-exist@text || div.book-item h3 a@text", html, "https://s.example/"),
        )
        assertNull(evaluator.string("nope@text || also-nope@text", html, "https://s.example/"))
    }

    @Test
    fun `conjunction joins with newlines`() = runTest {
        assertEquals(
            "斗破苍穹\n天蚕土豆",
            evaluator.string(
                "div.book-item h3 a@text && .book-item .author@text",
                html,
                "https://s.example/",
            ),
        )
    }

    @Test
    fun `trailing regex replace applies`() = runTest {
        assertEquals(
            "斗破",
            evaluator.string("div.book-item h3 a@text##苍穹", html, "https://s.example/"),
        )
        assertEquals(
            "斗破xx",
            evaluator.string("div.book-item h3 a@text##苍穹##xx", html, "https://s.example/"),
        )
    }

    @Test
    fun `inline js interpolation evaluates expressions`() = runTest {
        assertEquals("2", evaluator.string("{{1 + 1}}", html, "https://s.example/"))
        assertEquals(
            "v=https://s.example/",
            evaluator.string("v={{baseUrl}}", html, "https://s.example/"),
        )
    }

    @Test
    fun `whole-rule js blocks run`() = runTest {
        assertEquals(
            "a|b",
            evaluator.itemString("<js>'a' + '|' + 'b'</js>", element = null, jsonText = null, baseUrl = "https://s.example/"),
        )
        assertEquals(
            "x",
            evaluator.itemString("@js:'x'", element = null, jsonText = null, baseUrl = "https://s.example/"),
        )
    }

    @Test
    fun `top-level separators are not split inside js regions`() = runTest {
        // The || inside the JS expression must not act as a fallback separator; the JS value
        // wins over the later alternative.
        assertEquals("a||b", evaluator.string("{{'a||b'}} || nope@text", html, "https://s.example/"))
        assertEquals("a||b", evaluator.string("<js>'a||b'</js> || nope@text", html, "https://s.example/"))
    }

    @Test
    fun `json paths extract values and arrays`() = runTest {
        val json = """{"data":{"list":[{"name":"书一","url":"/b/1"},{"name":"书二","url":"/b/2"}]}}"""

        assertEquals("书一", evaluator.string("$.data.list[0].name", json, "https://api.example/"))
        assertEquals("书一\n书二", evaluator.string("$.data.list[*].name", json, "https://api.example/"))
        assertEquals("/b/1\n/b/2", evaluator.string("$.data.list[*].url", json, "https://api.example/"))
        assertNull(evaluator.string("$.data.missing", json, "https://api.example/"))
    }

    @Test
    fun `json list rule returns each item`() = runTest {
        val json = """{"data":{"list":[{"name":"书一","url":"/b/1"},{"name":"书二","url":"/b/2"}]}}"""

        val items = evaluator.jsonList("$.data.list[*]", json)

        assertEquals(2, items.size)
        assertTrue(items.all { it.contains("\"name\"") })
    }

    @Test
    fun `list rule returns html elements`() = runTest {
        val items = evaluator.list("div.book-item", html)

        assertEquals(2, items.size)
        assertEquals("斗破苍穹", items[0].select("h3 a").text())
    }
}
