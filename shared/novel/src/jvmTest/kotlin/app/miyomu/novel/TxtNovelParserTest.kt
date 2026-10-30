package app.miyomu.novel

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TxtNovelParserTest {

    private val parser = TxtNovelParser()

    private fun txt(vararg lines: String, charset: String = "UTF-8", bom: Boolean = false): ByteArray {
        val body = lines.joinToString("\n").toByteArray(charset(charset))
        return if (bom) byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body else body
    }

    @Test
    fun `splits UTF-8 file with Chinese chapter markers and front matter`() {
        val parsed = parser.parse(
            "测试书.txt",
            txt(
                "前言内容",
                "更多前言",
                "",
                "第一章 起点",
                "第一段内容。",
                "第二段内容。",
                "",
                "第二章 转折",
                "第二 chapter 内容",
            ),
        )

        assertEquals("测试书", parsed.title)
        assertEquals(3, parsed.chapters.size)
        assertEquals("开篇", parsed.chapters[0].title)
        assertEquals("第一章 起点", parsed.chapters[1].title)
        assertEquals("第二章 转折", parsed.chapters[2].title)
        assertEquals("前言内容\n更多前言", parsed.chapters[0].text())
        assertEquals("第一段内容。\n第二段内容。", parsed.chapters[1].text())
        assertEquals("第二 chapter 内容", parsed.chapters[2].text())
    }

    @Test
    fun `decodes GB18030 file without BOM`() {
        val parsed = parser.parse(
            "gb.txt",
            txt("第一章 中文", "GB18030 编码内容。", "第二章 结尾", "结尾内容。", charset = "GB18030"),
        )

        assertEquals(2, parsed.chapters.size)
        assertEquals("第一章 中文", parsed.chapters[0].title)
        assertEquals("GB18030 编码内容。", parsed.chapters[0].text())
        assertEquals("结尾内容。", parsed.chapters[1].text())
    }

    @Test
    fun `strips UTF-8 BOM`() {
        val parsed = parser.parse("bom.txt", txt("第一章 开始", "内容", bom = true))

        assertEquals(1, parsed.chapters.size)
        assertEquals("第一章 开始", parsed.chapters[0].title)
        assertEquals("内容", parsed.chapters[0].text())
    }

    @Test
    fun `folds volume divider into the following chapter`() {
        val parsed = parser.parse(
            "volume.txt",
            txt("第一卷 风起", "第一章 甲", "甲的内容", "第二章 乙", "乙的内容"),
        )

        assertEquals(2, parsed.chapters.size)
        assertEquals("第一卷 风起 第一章 甲", parsed.chapters[0].title)
        assertEquals("甲的内容", parsed.chapters[0].text())
        assertEquals("第二章 乙", parsed.chapters[1].title)
    }

    @Test
    fun `drops trailing marker without body`() {
        val parsed = parser.parse("trail.txt", txt("第一章 甲", "甲的内容", "第二章 乙"))

        assertEquals(1, parsed.chapters.size)
        assertEquals("第一章 甲", parsed.chapters[0].title)
    }

    @Test
    fun `falls back to single chapter when no markers`() {
        val parsed = parser.parse("plain.txt", txt("没有章节标记的一段文字。", "另一段。"))

        assertEquals(1, parsed.chapters.size)
        assertEquals("正文", parsed.chapters[0].title)
        assertEquals("没有章节标记的一段文字。\n另一段。", parsed.chapters[0].text())
    }

    @Test
    fun `recognizes special markers and English chapters`() {
        val parsed = parser.parse(
            "mixed.txt",
            txt("序章", "这一段是序章的正文内容", "Chapter 1: Begin", "english body"),
        )

        assertEquals(2, parsed.chapters.size)
        assertEquals("序章", parsed.chapters[0].title)
        assertEquals("Chapter 1: Begin", parsed.chapters[1].title)
        assertEquals("english body", parsed.chapters[1].text())
    }

    @Test
    fun `rejects files above the size limit`() {
        val tiny = TxtNovelParser(maxBytes = 10)

        assertThrows<NovelParseException> { tiny.parse("big.txt", ByteArray(11)) }
    }

    @Test
    fun `chapter text is blank-safe for empty bodies`() {
        val parsed = parser.parse("empty.txt", txt("第一章 甲", "", "甲的正文内容", "第二章 乙", "乙内容"))

        assertEquals(2, parsed.chapters.size)
        assertEquals("甲的正文内容", parsed.chapters[0].text())
        assertTrue(parsed.chapters[0].text().isNotBlank())
    }
}
