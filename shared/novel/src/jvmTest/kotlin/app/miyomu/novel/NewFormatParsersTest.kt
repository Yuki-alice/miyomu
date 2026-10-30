package app.miyomu.novel

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Fb2NovelParserTest {

    private val parser = Fb2NovelParser()

    // 24x36 PNG header, sniffable but not decodable — fine for parser assertions.
    private val artBytes = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52,
        0, 0, 0, 24, 0, 0, 0, 36, 8, 6, 0, 0, 0, 0, 0, 0, 0,
    )

    private val base64Art = java.util.Base64.getEncoder().encodeToString(artBytes)

    private val fb2 = """
        <?xml version="1.0" encoding="utf-8"?>
        <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">
          <description>
            <title-info>
              <author><first-name>甲</first-name><last-name>乙</last-name></author>
              <book-title>测试书</book-title>
              <coverpage><image l:href="#cover.png"/></coverpage>
            </title-info>
          </description>
          <body>
            <section><title><p>第一章 起点</p></title>
              <p>开篇内容。</p>
              <image l:href="#illus.png"/>
              <p>插画后的内容。</p>
            </section>
            <section><title><p>第二章 转折</p></title>
              <p>转折内容。</p>
            </section>
          </body>
          <binary id="illus.png" content-type="image/png">$base64Art</binary>
          <binary id="cover.png" content-type="image/png">$base64Art</binary>
        </FictionBook>
    """.trimIndent()

    @Test
    fun `parses metadata, section chapters, blocks and cover`() {
        val parsed = parser.parse(fb2.toByteArray(Charsets.UTF_8))

        assertEquals("测试书", parsed.title)
        assertEquals("甲 乙", parsed.author)
        assertEquals(2, parsed.chapters.size)

        assertEquals("第一章 起点", parsed.chapters[0].title)
        val blocks = parsed.chapters[0].blocks()
        // The section <title> is extracted as the chapter title (its escaped markup is not
        // walked), so the body holds: paragraph, illustration, paragraph.
        assertEquals(3, blocks.size)
        assertEquals(0, blocks[0].startChar)
        assertEquals("开篇内容。", (blocks[0] as NovelBlock.Text).text)
        val image = blocks[1] as NovelBlock.Image
        assertEquals(6, image.startChar)
        assertEquals("png", image.format)
        assertEquals(artBytes.size, image.bytes.size)
        assertEquals(24, image.width)
        assertEquals(36, image.height)
        assertEquals(6, blocks[2].startChar)
        assertEquals("插画后的内容。", (blocks[2] as NovelBlock.Text).text)
        assertEquals("开篇内容。\n插画后的内容。", parsed.chapters[0].text())

        assertEquals("第二章 转折", parsed.chapters[1].title)
        assertEquals("转折内容。", parsed.chapters[1].text())

        assertNotNull(parsed.coverBytes)
    }
}

class HtmlNovelParserTest {

    private val parser = HtmlNovelParser()

    @Test
    fun `extracts title, drops scripts and produces one chapter`() {
        val html = """
            <html><head><title>网文页面</title>
            <script>var tracking = 1;</script>
            <style>p { color: red }</style></head>
            <body>
            <h1>正文标题</h1>
            <p>第一段。</p>
            <p>第二段。</p>
            </body></html>
        """.trimIndent()

        val parsed = parser.parse("web-novel.html", html.toByteArray(Charsets.UTF_8))

        assertEquals("网文页面", parsed.title)
        assertEquals(1, parsed.chapters.size)
        assertEquals("正文标题", parsed.chapters[0].title)
        assertEquals("正文标题\n第一段。\n第二段。", parsed.chapters[0].text())
        assertTrue(parsed.chapters[0].text().contains("第一段。"))
        // Script/style content must not leak into the reading text.
        assertTrue(!parsed.chapters[0].text().contains("tracking"))
        assertTrue(!parsed.chapters[0].text().contains("color: red"))
    }

    @Test
    fun `empty body yields no chapters`() {
        val parsed = parser.parse("blank.html", "<html><body></body></html>".toByteArray())
        assertTrue(parsed.chapters.isEmpty())
    }
}

class MarkdownNovelParserTest {

    private val parser = MarkdownNovelParser()

    @Test
    fun `headings start chapters and body stays verbatim`() {
        val md = """
            # 卷一 开端

            第一段内容。

            第二段内容。

            ## 第二章 深入

            继续内容,含 **粗体** 与 [链接](https://example.com),原样保留。
        """.trimIndent()

        val parsed = parser.parse("fan-translation.md", md.toByteArray(Charsets.UTF_8))

        assertEquals("卷一 开端", parsed.title)
        assertEquals(2, parsed.chapters.size)
        assertEquals("卷一 开端", parsed.chapters[0].title)
        assertEquals("第一段内容。\n\n第二段内容。", parsed.chapters[0].text())
        assertEquals("第二章 深入", parsed.chapters[1].title)
        assertTrue(parsed.chapters[1].text().contains("**粗体**"))
    }

    @Test
    fun `no headings falls back to a single chapter`() {
        val parsed = parser.parse("plain-notes.md", "随手记的正文。".toByteArray())

        assertEquals("plain-notes", parsed.title)
        assertEquals(1, parsed.chapters.size)
        assertNull(parsed.chapters[0].title)
        assertEquals("随手记的正文。", parsed.chapters[0].text())
    }
}
