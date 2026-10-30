package app.miyomu.local

import app.miyomu.model.MediaType
import java.io.File
import java.net.URL
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** P1-6 OPDS 服务化: feed serving, book download and the traversal guard over a real socket. */
class OpdsServerTest {

    @TempDir
    lateinit var root: File

    private fun book(title: String, location: String) = LocalBook(
        id = location,
        title = title,
        format = LocalBookFormat.ZIP,
        mediaType = MediaType.COMIC,
        location = location,
        lastModified = 1L,
    )

    @Test
    fun `serves catalog and book files and blocks traversal`() {
        val exportRoot = File(root, "export").apply { mkdirs() }
        val booksDir = File(exportRoot, "books").apply { mkdirs() }
        val payload = byteArrayOf(1, 2, 3, 4)
        File(booksDir, "book1.cbz").writeBytes(payload)
        val books = listOf(book("book1", File(booksDir, "book1.cbz").absolutePath))

        val handle = runBlocking {
            startOpdsServer(port = 0, exportRoot = exportRoot, title = "Lib", booksProvider = { books }, nowMillis = { 1L })
        }
        try {
            val base = "http://127.0.0.1:${handle.port}"

            val catalog = URL("$base/catalog.xml").readText()
            assertTrue("opds-catalog:kind=acquisition" in catalog)
            assertTrue("book1.cbz" in catalog)

            val downloaded = URL("$base/books/book1.cbz").readBytes()
            assertEquals(4, downloaded.size)

            // Path traversal is rejected (404 body).
            val traversal = runCatching {
                URL("$base/books/..%2Fcatalog.xml").readText()
            }
            assertTrue(traversal.isFailure || "opds-catalog" !in traversal.getOrDefault(""))
        } finally {
            handle.stop()
        }
    }
}
