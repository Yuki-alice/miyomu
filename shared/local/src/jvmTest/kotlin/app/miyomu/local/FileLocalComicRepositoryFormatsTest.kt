package app.miyomu.local

import app.miyomu.model.MediaType
import java.io.File
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * P0-1 regression net: a scanned 7z/CB7/PDF book must be openable end to end — chapters, the
 * page list, page bytes and the generated cover — on the JVM target.
 */
class FileLocalComicRepositoryFormatsTest {

    @TempDir
    lateinit var root: File

    private val repository = FileLocalComicRepository()
    private val scanner = LocalLibraryScanner()

    @Test
    fun `seven zip book lists and reads pages in natural order`() = runBlocking {
        writeSevenZip(File(root, "scan.cb7"), listOf("10.jpg", "2.jpg", "1.jpg"))

        val book = scanner.scan(FileLocalFileRef(root)).single()
        assertEquals(LocalBookFormat.SEVEN_ZIP, book.format)
        assertEquals(MediaType.COMIC, book.mediaType)

        val chapter = repository.chapters(book).single()
        assertEquals(LocalBookFormat.SEVEN_ZIP, chapter.format)

        val pages = repository.pages(chapter)
        assertEquals(listOf("1.jpg", "2.jpg", "10.jpg"), pages.map { it.name })

        assertEquals(contentByte("1.jpg"), repository.readPage(pages.first()).single())
        assertEquals(contentByte("10.jpg"), repository.readPage(pages.last()).single())

        val cover = repository.cover(book)
        assertNotNull(cover)
        assertEquals(contentByte("1.jpg"), cover.single())
    }

    @Test
    fun `seven zip directory entries are skipped`() = runBlocking {
        writeSevenZip(File(root, "book.7z"), listOf("p1.jpg", "cover/", "nested/p2.jpg"))

        val book = scanner.scan(FileLocalFileRef(root)).single()
        val pages = repository.pages(repository.chapters(book).single())
        // The `cover/` directory entry is excluded; names sort by natural compare.
        assertEquals(listOf("nested/p2.jpg", "p1.jpg"), pages.map { it.name })
    }

    @Test
    fun `pdf book renders pages and cover`() = runBlocking {
        writePdf(File(root, "manual.pdf"), pageCount = 3)

        val book = scanner.scan(FileLocalFileRef(root)).single()
        assertEquals(LocalBookFormat.PDF, book.format)
        assertEquals(MediaType.COMIC, book.mediaType)

        val chapter = repository.chapters(book).single()
        val pages = repository.pages(chapter)
        assertEquals(3, pages.size)

        val first = repository.readPage(pages.first())
        assertEquals(0xFF, first[0].toInt() and 0xFF)
        assertEquals(0xD8, first[1].toInt() and 0xFF)

        val last = repository.readPage(pages.last())
        assertTrue(last.size > 4)

        assertNotNull(repository.cover(book))
    }

    @Test
    fun `directory book opens nested cb7 chapters`() = runBlocking {
        val bookDir = File(root, "anthology").apply { mkdirs() }
        writeSevenZip(File(bookDir, "chapter 2.cb7"), listOf("1.jpg", "2.jpg"))

        val book = scanner.scan(FileLocalFileRef(root)).single()
        assertEquals(LocalBookFormat.DIRECTORY, book.format)

        val chapter = repository.chapters(book).single()
        assertEquals("chapter 2", chapter.title)
        assertEquals(LocalBookFormat.SEVEN_ZIP, chapter.format)

        val pages = repository.pages(chapter)
        assertEquals(listOf("1.jpg", "2.jpg"), pages.map { it.name })
        assertEquals(contentByte("2.jpg"), repository.readPage(pages.last()).single())
    }

    /** One identifiable content byte per entry: the first character of its name. */
    private fun contentByte(name: String): Byte = name.first().code.toByte()

    private fun writeSevenZip(target: File, entries: List<String>) {
        SevenZOutputFile(target).use { out ->
            entries.forEach { name ->
                val directory = name.endsWith("/")
                val entry = SevenZArchiveEntry().apply {
                    this.name = name
                    isDirectory = directory
                    setSize(if (directory) 0L else 1L)
                }
                out.putArchiveEntry(entry)
                if (!directory) out.write(byteArrayOf(contentByte(name)))
                out.closeArchiveEntry()
            }
        }
    }

    private fun writePdf(target: File, pageCount: Int) {
        PDDocument().use { doc ->
            repeat(pageCount) { index ->
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                PDPageContentStream(doc, page).use { content ->
                    content.beginText()
                    content.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 24f)
                    content.newLineAtOffset(72f, 700f)
                    content.showText("Page ${index + 1}")
                    content.endText()
                }
            }
            doc.save(target)
        }
    }
}
