package app.miyomu.novel

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EpubNovelParserTest {

    private val parser = EpubNovelParser()

    private fun epub(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, data) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun t(text: String): ByteArray = text.toByteArray(Charsets.UTF_8)

    private val container = """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
    """.trimIndent()

    private val opfWithCover = """
        <?xml version="1.0"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="id">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title>测试小说</dc:title>
            <dc:creator>作者甲</dc:creator>
            <meta name="cover" content="cover-img"/>
          </metadata>
          <manifest>
            <item id="cover-img" href="cover.jpg" media-type="image/jpeg"/>
            <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
            <item id="c2" href="sub/ch2.xhtml" media-type="application/xhtml+xml"/>
            <item id="css" href="style.css" media-type="text/css"/>
          </manifest>
          <spine>
            <itemref idref="c1"/>
            <itemref idref="c2"/>
          </spine>
        </package>
    """.trimIndent()

    private val ch1 = """
        <html><head><title>第一章 起点</title></head><body>
        <h1>第一章 起点</h1>
        <p>段落一。</p>
        <p>段落二 <b>加粗</b>。</p>
        </body></html>
    """.trimIndent()

    private val ch2 = """
        <html><body>
        <p>第二节的内容。</p>
        </body></html>
    """.trimIndent()

    private val coverBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

    @Test
    fun `parses metadata, chapters, paragraphs and cover`() {
        val parsed = parser.parse(
            epub(
                "META-INF/container.xml" to t(container),
                "OEBPS/content.opf" to t(opfWithCover),
                "OEBPS/ch1.xhtml" to t(ch1),
                "OEBPS/sub/ch2.xhtml" to t(ch2),
                "OEBPS/cover.jpg" to coverBytes,
                "OEBPS/style.css" to t("body { color: black }"),
            ),
        )

        assertEquals("测试小说", parsed.title)
        assertEquals("作者甲", parsed.author)
        assertEquals(2, parsed.chapters.size)

        assertEquals("第一章 起点", parsed.chapters[0].title)
        // Headings stay part of the reading text, like TXT files include their title line.
        assertEquals("第一章 起点\n段落一。\n段落二 加粗。", parsed.chapters[0].text())

        // Chapter without headings falls back to a positional title.
        assertEquals("第 2 节", parsed.chapters[1].title)
        assertEquals("第二节的内容。", parsed.chapters[1].text())

        assertNotNull(parsed.coverBytes)
        assertEquals(coverBytes.size, parsed.coverBytes.size)
    }

    @Test
    fun `interleaves illustration blocks with aligned offsets`() {
        val illustration = pngHeader(width = 24, height = 36)
        val chapterWithArt = """
            <html><head><title>插图章</title></head><body>
            <h1>插图章</h1>
            <p>插画前的段落。</p>
            <div class="illustration"><img src="art/pic.png" alt=""/></div>
            <p>插画后的段落。</p>
            </body></html>
        """.trimIndent()
        val opf = opfWithCover
            .replace("""<item id="cover-img" href="cover.jpg" media-type="image/jpeg"/>""", "")
            .replace("""<meta name="cover" content="cover-img"/>""", "")
            .replace("""<item id="c2" href="sub/ch2.xhtml" media-type="application/xhtml+xml"/>""", "")
            .replace("""<itemref idref="c2"/>""", "")
            .replace(
                """<item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>""",
                """<item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
                   <item id="art" href="art/pic.png" media-type="image/png"/>""",
            )

        val parsed = parser.parse(
            epub(
                "META-INF/container.xml" to t(container),
                "OEBPS/content.opf" to t(opf),
                "OEBPS/ch1.xhtml" to t(chapterWithArt),
                "OEBPS/art/pic.png" to illustration,
            ),
        )

        assertEquals(1, parsed.chapters.size)
        val blocks = parsed.chapters[0].blocks()
        assertEquals(4, blocks.size)
        // The h1 heading and the paragraphs are Text blocks; the illustration sits between.
        val heading = blocks[0] as NovelBlock.Text
        val paragraph = blocks[1] as NovelBlock.Text
        val image = blocks[2] as NovelBlock.Image
        val last = blocks[3] as NovelBlock.Text

        assertEquals(0, heading.startChar)
        assertEquals("插图章", heading.text)
        assertEquals(4, paragraph.startChar)
        assertEquals("插画前的段落。", paragraph.text)

        assertEquals(12, image.startChar) // after "插图章\n插画前的段落。" + newline
        assertEquals(illustration.size, image.bytes.size)
        assertEquals("png", image.format)
        assertEquals(24, image.width)
        assertEquals(36, image.height)

        // text() excludes images; offsets must align with the joined text stream.
        assertEquals("插图章\n插画前的段落。\n插画后的段落。", parsed.chapters[0].text())
        assertEquals(12, last.startChar)
        assertEquals("插画后的段落。", last.text)
    }

    /** Minimal PNG: signature + IHDR header with the given dimensions, no real pixel data. */
    private fun pngHeader(width: Int, height: Int): ByteArray = buildList {
        addAll(
            byteArrayOf(
                0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 13,
            ).toList(),
        )
        addAll("IHDR".toByteArray().toList())
        addAll(
            byteArrayOf(
                (width shr 24).toByte(), (width shr 16).toByte(), (width shr 8).toByte(), width.toByte(),
                (height shr 24).toByte(), (height shr 16).toByte(), (height shr 8).toByte(), height.toByte(),
                8, 6, 0, 0, 0,
            ).toList(),
        )
        addAll(byteArrayOf(0, 0, 0, 0).toList()) // CRC placeholder, not needed for sniffing
    }.toByteArray()

    @Test
    fun `finds EPUB3 cover via properties attribute`() {
        val opf = """
            <?xml version="1.0"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>EPUB3 书</dc:title>
              </metadata>
              <manifest>
                <item id="cover3" href="cover3.jpg" media-type="image/jpeg" properties="cover-image"/>
                <item id="c1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
              </manifest>
              <spine><itemref idref="c1"/></spine>
            </package>
        """.trimIndent()
        val image = byteArrayOf(0x01, 0x02, 0x03)

        val parsed = parser.parse(
            epub(
                "META-INF/container.xml" to t(container),
                "OEBPS/content.opf" to t(opf),
                "OEBPS/ch1.xhtml" to t(ch1),
                "OEBPS/cover3.jpg" to image,
            ),
        )

        assertEquals("EPUB3 书", parsed.title)
        assertNotNull(parsed.coverBytes)
        assertEquals(image.size, parsed.coverBytes.size)
    }

    @Test
    fun `reports chapters without cover as null`() {
        val opf = opfWithCover
            .replace("""<meta name="cover" content="cover-img"/>""", "")
            .replace("""<item id="cover-img" href="cover.jpg" media-type="image/jpeg"/>""", "")

        val parsed = parser.parse(
            epub(
                "META-INF/container.xml" to t(container),
                "OEBPS/content.opf" to t(opf),
                "OEBPS/ch1.xhtml" to t(ch1),
                "OEBPS/sub/ch2.xhtml" to t(ch2),
            ),
        )

        assertNull(parsed.coverBytes)
    }

    @Test
    fun `throws on missing container`() {
        assertThrows<NovelParseException> { parser.parse(epub("other.txt" to t("x"))) }
    }
}
