package app.miyomu.local

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalLibraryScannerTest {

    @TempDir
    lateinit var root: File

    private val repository = FileLocalComicRepository()

    @Test
    fun `scans directories and zip books in natural order`() {
        val book10 = File(root, "book10").apply { mkdirs() }
        File(book10, "chapter 2").apply { mkdirs() }
        writeFile(File(book10, "chapter 2"), "1.jpg", byteArrayOf(1))
        File(book10, "chapter 10").apply { mkdirs() }
        writeFile(File(book10, "chapter 10"), "1.jpg", byteArrayOf(1))
        writeZip(File(root, "book2.zip"), listOf("10.jpg", "2.jpg", "1.jpg"))
        writeFile(root, "ignored.pdf", byteArrayOf(1))

        val books = LocalLibraryScanner().scan(FileLocalFileRef(root))

        assertEquals(listOf("book2", "book10"), books.map { it.title })
        assertEquals(LocalBookFormat.ZIP, books[0].format)
        assertEquals(LocalBookFormat.DIRECTORY, books[1].format)
    }

    @Test
    fun `directory pages are natural sorted and readable`() = kotlinx.coroutines.runBlocking {
        val directory = File(root, "book").apply { mkdirs() }
        writeFile(directory, "10.jpg", byteArrayOf(10))
        writeFile(directory, "2.jpg", byteArrayOf(2))
        writeFile(directory, "1.jpg", byteArrayOf(1))
        writeFile(directory, "notes.txt", byteArrayOf(0))

        val book = LocalLibraryScanner().scan(FileLocalFileRef(root)).single()
        val chapters = repository.chapters(book)
        assertEquals(1, chapters.size)
        assertEquals(book.title, chapters.single().title)

        val pages = repository.pages(chapters.single())
        assertEquals(listOf("1.jpg", "2.jpg", "10.jpg"), pages.map { it.name })
        assertEquals(1, repository.readPage(pages.first()).single())

        val cover = repository.cover(book)
        assertNotNull(cover)
        assertEquals(1, cover.single())
    }

    @Test
    fun `zip pages are natural sorted and readable`() = kotlinx.coroutines.runBlocking {
        val archive = File(root, "book.cbz")
        writeZip(archive, listOf("10.jpg", "2.jpg", "1.jpg", "cover.png"))

        val book = LocalLibraryScanner().scan(FileLocalFileRef(root)).single()
        val chapter = repository.chapters(book).single()
        val pages = repository.pages(chapter)

        assertEquals(listOf("1.jpg", "2.jpg", "10.jpg", "cover.png"), pages.map { it.name })
        assertTrue(pages.all { it.location.contains("!/") })
        assertEquals(7, repository.readPage(pages.first()).single())
    }

    @Test
    fun `missing root returns empty scan`() {
        val books = LocalLibraryScanner().scan(FileLocalFileRef(File(root, "missing")))
        assertTrue(books.isEmpty())
    }

    private fun writeFile(parent: File, name: String, bytes: ByteArray) {
        parent.mkdirs()
        Files.write(parent.toPath().resolve(name), bytes)
    }

    private fun writeZip(target: File, names: List<String>) {
        ZipOutputStream(target.outputStream()).use { zip ->
            names.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(7)
                zip.closeEntry()
            }
        }
    }
}
