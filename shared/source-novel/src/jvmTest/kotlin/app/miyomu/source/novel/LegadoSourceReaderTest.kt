package app.miyomu.source.novel

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LegadoSourceReaderTest {

    private val sourceJson = """
        {
          "bookSourceUrl": "https://s.example",
          "bookSourceName": "示例书源",
          "bookSourceGroup": "测试",
          "searchUrl": "https://s.example/search?q={{key}}&p={{page}}",
          "ruleSearch": {
            "bookList": "div.book-item",
            "name": "h3 a@text",
            "author": ".author@text",
            "bookUrl": "h3 a@href",
            "coverUrl": "img@src",
            "intro": ".intro@text"
          },
          "ruleBookInfo": {
            "name": "h1@text",
            "author": "#author@text",
            "intro": "#intro@text",
            "coverUrl": "#cover@src",
            "tocUrl": "#toclink@href",
            "lastChapter": "#last@text"
          },
          "ruleToc": {
            "chapterList": "ul.chapters li",
            "chapterName": "a@text",
            "chapterUrl": "a@href"
          },
          "ruleContent": {
            "content": "#content@textNodes"
          }
        }
    """.trimIndent()

    private val reader = LegadoSourceReader(LegadoBookSource.fromJson(sourceJson), LegadoQuickJsEngine())

    private val searchPage = """
        <html><body>
          <div class="book-item">
            <h3><a href="/book/1">斗破苍穹</a></h3>
            <span class="author">天蚕土豆</span>
            <img src="/cover/1.jpg"/>
            <div class="intro">一段简介。</div>
          </div>
          <div class="book-item">
            <h3><a href="/book/2">凡人修仙传</a></h3>
            <span class="author">忘语</span>
          </div>
        </body></html>
    """.trimIndent()

    private val infoPage = """
        <html><body>
          <h1>斗破苍穹</h1>
          <span id="author">天蚕土豆</span>
          <p id="intro">这是一个简介。</p>
          <img id="cover" src="/cover/1.jpg"/>
          <a id="toclink" href="/book/1/toc">目录页</a>
          <span id="last">第一千章 大结局</span>
        </body></html>
    """.trimIndent()

    private val tocPage = """
        <html><body>
          <ul class="chapters">
            <li><a href="/book/1/c1">第一章 起点</a></li>
            <li><a href="/book/1/c2">第二章 转折</a></li>
          </ul>
        </body></html>
    """.trimIndent()

    private val contentPage = """
        <html><body><div id="content">
          <p>第一段正文。</p>
          <p>第二段正文。</p>
        </div></body></html>
    """.trimIndent()

    @Test
    fun `search url template substitutes key and page`() {
        assertEquals(
            "https://s.example/search?q=%E6%96%97%E7%A0%B4&p=2",
            reader.searchUrl(keyword = "斗破", page = 2),
        )
    }

    @Test
    fun `search parses html result lists with absolute urls`() = runTest {
        val books = reader.search(searchPage, "https://s.example/search?q=x")

        assertEquals(2, books.size)
        val first = books[0]
        assertEquals("斗破苍穹", first.name)
        assertEquals("天蚕土豆", first.author)
        assertEquals("https://s.example/book/1", first.bookUrl)
        assertEquals("https://s.example/cover/1.jpg", first.coverUrl)
        assertEquals("一段简介。", first.intro)
        assertEquals("凡人修仙传", books[1].name)
    }

    private val exploreSourceJson = """
        {
          "bookSourceUrl": "https://s.example",
          "bookSourceName": "发现源",
          "exploreUrl": "玄幻::/list/xh?p={{page}}\n都市::https://s.example/list/ds",
          "ruleExplore": {
            "bookList": "div.book-item",
            "name": "h3 a@text",
            "author": ".author@text",
            "bookUrl": "h3 a@href",
            "coverUrl": "img@src",
            "intro": ".intro@text"
          }
        }
    """.trimIndent()

    @Test
    fun `explore categories parse from lines and json`() {
        val lineSource = LegadoBookSource.fromJson(exploreSourceJson)
        assertEquals(
            listOf("玄幻" to "/list/xh?p={{page}}", "都市" to "https://s.example/list/ds"),
            lineSource.exploreCategories().map { it.title to it.url },
        )

        val jsonSource = LegadoBookSource.fromJson(
            """
            {
              "bookSourceUrl": "https://s.example",
              "exploreUrl": [
                { "title": "科幻", "url": "/list/kh" },
                { "name": "悬疑", "url": "/list/xy" }
              ]
            }
            """.trimIndent(),
        )
        assertEquals(
            listOf("科幻" to "/list/kh", "悬疑" to "/list/xy"),
            jsonSource.exploreCategories().map { it.title to it.url },
        )
    }

    @Test
    fun `explore url substitutes page and resolves against the source`() {
        val source = LegadoBookSource.fromJson(exploreSourceJson)
        val exploreReader = LegadoSourceReader(source, LegadoQuickJsEngine())
        val category = source.exploreCategories().first()

        assertEquals(
            "https://s.example/list/xh?p=2",
            exploreReader.exploreUrl(category, page = 2),
        )
    }

    @Test
    fun `explore parses lists with explore rules`() = runTest {
        val exploreReader = LegadoSourceReader(
            LegadoBookSource.fromJson(exploreSourceJson),
            LegadoQuickJsEngine(),
        )

        val books = exploreReader.explore(searchPage, "https://s.example/list/xh?p=1")

        assertEquals(2, books.size)
        assertEquals("斗破苍穹", books[0].name)
        assertEquals("天蚕土豆", books[0].author)
        assertEquals("https://s.example/book/1", books[0].bookUrl)
        assertEquals("https://s.example/cover/1.jpg", books[0].coverUrl)
        assertEquals("一段简介。", books[0].intro)
    }

    @Test
    fun `book info extracts fields and defaults toc url to the page`() = runTest {
        val info = reader.bookInfo(infoPage, "https://s.example/book/1")

        assertTrue(info != null)
        assertEquals("斗破苍穹", info.name)
        assertEquals("天蚕土豆", info.author)
        assertEquals("这是一个简介。", info.intro)
        assertEquals("https://s.example/cover/1.jpg", info.coverUrl)
        assertEquals("https://s.example/book/1/toc", info.tocUrl)
        assertEquals("第一千章 大结局", info.latestChapter)
    }

    @Test
    fun `toc lists chapters with absolute urls`() = runTest {
        val chapters = reader.toc(tocPage, "https://s.example/book/1/toc")

        assertEquals(2, chapters.size)
        assertEquals("第一章 起点", chapters[0].title)
        assertEquals("https://s.example/book/1/c1", chapters[0].url)
    }

    @Test
    fun `content joins paragraphs and trims blanks`() = runTest {
        val text = reader.content(contentPage, "https://s.example/book/1/c1")

        assertEquals("第一段正文。\n第二段正文。", text)
    }

    @Test
    fun `json responses are supported end to end`() = runTest {
        val apiSource = LegadoBookSource.fromJson(
            """
            {
              "bookSourceUrl": "https://api.example",
              "searchUrl": "https://api.example/search?q={{key}}",
              "ruleSearch": {
                "bookList": "$.data.list[*]",
                "name": "$.name",
                "author": "$.author",
                "bookUrl": "$.url"
              },
              "ruleToc": { "chapterList": "$.chapters[*]", "chapterName": "$.title", "chapterUrl": "$.url" },
              "ruleContent": { "content": "$.content" }
            }
            """.trimIndent(),
        )
        val apiReader = LegadoSourceReader(apiSource, LegadoQuickJsEngine())

        val searchBody = """{"data":{"list":[{"name":"书一","author":"作者一","url":"/b/1"}]}}"""
        val books = apiReader.search(searchBody, "https://api.example/search?q=x")
        assertEquals(1, books.size)
        assertEquals("书一", books[0].name)
        assertEquals("https://api.example/b/1", books[0].bookUrl)

        val tocBody = """{"chapters":[{"title":"第一章","url":"/b/1/c1"},{"title":"第二章","url":"/b/1/c2"}]}"""
        val chapters = apiReader.toc(tocBody, "https://api.example/b/1/toc")
        assertEquals(2, chapters.size)
        assertEquals("https://api.example/b/1/c1", chapters[0].url)

        val contentBody = """{"content":"第一段。\n第二段。"}"""
        assertEquals("第一段。\n第二段。", apiReader.content(contentBody, "https://api.example/b/1/c1"))
    }

    @Test
    fun `source lists parse from arrays and single objects`() {
        val single = LegadoBookSource.listFromJson("""{"bookSourceName":"A"}""")
        val array = LegadoBookSource.listFromJson("""[{"bookSourceName":"A"},{"bookSourceName":"B"}]""")

        assertEquals(listOf("A"), single.map { it.bookSourceName })
        assertEquals(listOf("A", "B"), array.map { it.bookSourceName })
    }
}
