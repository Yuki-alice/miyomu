package app.miyomu.local

import app.miyomu.model.MediaType
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** P1-6 OPDS 导出: feed structure, XML escaping, and the book copy layout. */
class OpdsCatalogTest {

    @TempDir
    lateinit var root: File

    private fun book(title: String, location: String, format: LocalBookFormat = LocalBookFormat.ZIP) =
        LocalBook(
            id = location,
            title = title,
            format = format,
            mediaType = MediaType.COMIC,
            location = location,
            lastModified = 1_700_000_000_000L,
        )

    @Test
    fun `feed contains acquisition entries with escaped titles`() {
        val feed = OpdsCatalog.buildAcquisitionFeed(
            title = "My Library <v1>",
            books = listOf(book("A & B", "/lib/a & b.cbz")),
            generatedAtMillis = 1_700_000_000_000L,
        )

        assertTrue("opds-catalog:kind=acquisition" in feed)
        assertTrue("<title>My Library &lt;v1&gt;</title>" in feed)
        assertTrue("<title>A &amp; B</title>" in feed)
        assertTrue("<id>urn:miyomu:library</id>" in feed)
    }

    @Test
    fun `empty library yields a valid feed without entries`() {
        val feed = OpdsCatalog.buildAcquisitionFeed("空书库", emptyList(), 1_700_000_000_000L)

        assertTrue("<id>urn:miyomu:library</id>" in feed)
        assertTrue("<entry>" !in feed)
    }

    @Test
    fun `export copies books and writes the catalog`() {
        val archive = File(root, "book1.cbz").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val books = listOf(book("book1", archive.absolutePath))

        val exportRoot = OpdsCatalog.export(File(root, "out"), "Lib", books, nowMillis = 42L)

        assertTrue(File(exportRoot, "catalog.xml").isFile)
        val copied = File(exportRoot, "books/book1.cbz")
        assertTrue(copied.isFile)
        assertEquals(3L, copied.length())
        val catalog = File(exportRoot, "catalog.xml").readText()
        assertTrue("books/book1.cbz" in catalog)
    }

    @Test
    fun `import restores an export into another library with conflict avoidance`() {
        val archive = File(root, "book1.cbz").apply { writeBytes(byteArrayOf(9, 9)) }
        val exportRoot = OpdsCatalog.export(
            File(root, "export"),
            "Lib",
            listOf(book("book1", archive.absolutePath)),
            nowMillis = 1L,
        )
        val libraryRoot = File(root, "library").apply { mkdirs() }

        assertEquals(1, OpdsCatalog.import(exportRoot, libraryRoot))
        assertEquals(2, File(libraryRoot, "book1.cbz").length())

        // Re-import with a changed file does not overwrite; a conflict copy lands instead.
        archive.writeBytes(byteArrayOf(1))
        OpdsCatalog.export(exportRoot, "Lib", listOf(book("book1", archive.absolutePath)), nowMillis = 2L)
        assertEquals(1, OpdsCatalog.import(exportRoot, libraryRoot))
        // The pre-existing shelf file stays intact; the changed copy lands as a conflict file.
        assertEquals(2L, File(libraryRoot, "book1.cbz").length())
        assertEquals(1L, File(libraryRoot, "book1 (1).cbz").length())
    }

    @Test
    fun `import of a missing catalog returns zero`() {
        assertEquals(0, OpdsCatalog.import(File(root, "missing"), File(root, "library")))
    }

    @Test
    fun `export file names sanitize unsafe characters`() {
        assertEquals("a_b.cbz", OpdsCatalog.exportFileName(book("a:b", "/lib/a:b.cbz")))
        assertEquals("目录", OpdsCatalog.exportFileName(book("目录", "/lib/目录").copy(format = LocalBookFormat.DIRECTORY)))
    }
}
