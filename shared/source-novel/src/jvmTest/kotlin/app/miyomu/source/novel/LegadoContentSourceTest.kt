package app.miyomu.source.novel

import app.miyomu.novel.NovelBlock
import app.miyomu.novel.NovelChapterCache
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
    fun `follows nextContentUrl across pages`() = runTest {
        val pagedSourceJson = sourceJson.replace(
            "\"ruleContent\": { \"content\": \"#content@textNodes\" }",
            "\"ruleContent\": { \"content\": \"#content@textNodes\", \"nextContentUrl\": \"#next@href\" }",
        )
        val bodies = mutableMapOf(
            "https://s.example/b/1/toc" to tocPage,
            "https://s.example/b/1/c1" to """
                <html><body>
                  <div id="content"><p>上半段。</p></div>
                  <a id="next" href="/b/1/c1?page=2">下一页</a>
                </body></html>
            """.trimIndent(),
            "https://s.example/b/1/c1?page=2" to contentPage("下半段。"),
        )
        val contentSource = LegadoContentSource(
            source = LegadoBookSource.fromJson(pagedSourceJson),
            book = LegadoOnlineBook(
                name = "测试在线书",
                author = "作者",
                bookUrl = "https://s.example/b/1",
                tocUrl = "https://s.example/b/1/toc",
                coverUrl = null,
                sourceUrl = "https://s.example",
            ),
            fetch = { url -> bodies[url] ?: error("no fixture for $url") },
            js = LegadoQuickJsEngine(),
        )

        val data = contentSource.load("book-key", 0, 0)

        assertNull(data.error)
        assertEquals("上半段。\n下半段。", data.text)
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


/** In-memory [NovelChapterCache] standing in for the disk store in cache-behavior tests. */
private class MemoryNovelCache : NovelChapterCache {
    val entries = mutableMapOf<Pair<String, String>, String>()

    override fun isCached(itemId: String, chapterKey: String): Boolean =
        entries.containsKey(itemId to chapterKey)

    override fun read(itemId: String, chapterKey: String): String? = entries[itemId to chapterKey]

    override fun write(itemId: String, chapterKey: String, text: String) {
        entries[itemId to chapterKey] = text
    }
}

class LegadoContentSourceCacheTest {

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

    private fun contentSource(
        cache: NovelChapterCache,
        bodies: MutableMap<String, String>,
    ): LegadoContentSource = LegadoContentSource(
        source = LegadoBookSource.fromJson(sourceJson),
        book = LegadoOnlineBook(
            name = "测试在线书",
            author = "作者",
            bookUrl = "https://s.example/b/1",
            tocUrl = "https://s.example/b/1/toc",
            coverUrl = null,
            sourceUrl = "https://s.example",
        ),
        fetch = { url -> bodies[url] ?: error("network down: $url") },
        js = LegadoQuickJsEngine(),
        cache = cache,
    )

    @Test
    fun `prefetched chapters serve without network`() = runTest {
        val cache = MemoryNovelCache()
        val bodies = mutableMapOf(
            "https://s.example/b/1/toc" to tocPage,
            "https://s.example/b/1/c1" to contentPage("第一段正文。"),
            "https://s.example/b/1/c2" to contentPage("第二段正文。"),
        )
        val online = contentSource(cache, bodies)
        // 缓存全本: sequential prefetch through the offline-first source.
        online.load("book-key", 0, 0)
        online.load("book-key", 1, 0)
        assertEquals(3, cache.entries.size) // toc + two chapters

        // Offline: network gone and a brand new source instance (as after an app restart).
        bodies.clear()
        val offline = contentSource(cache, bodies)
        val chapter = offline.load("book-key", 1, 0)
        assertNull(chapter.error)
        assertEquals("第二段正文。", chapter.text)
        assertEquals(2, chapter.chapterCount)
        assertEquals("第二章 转折", chapter.chapterTitle)
    }

    @Test
    fun `fetch failures still surface when nothing is cached`() = runTest {
        val cache = MemoryNovelCache()
        val contentSource = contentSource(cache, mutableMapOf())

        val data = contentSource.load("book-key", 0, 0)

        assertTrue(data.error != null)
        assertEquals("", data.text)
        assertTrue(cache.entries.isEmpty())
    }
}
