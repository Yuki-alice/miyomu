package app.miyomu.local

import app.miyomu.model.MediaType
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalLibraryMaintenanceTest {

    @TempDir
    lateinit var root: File

    private val maintenance = LocalLibraryMaintenance()

    @Test
    fun `audits size, duplicates and missing books`() {
        val bookA = File(root, "alpha").apply { mkdirs() }
        File(bookA, "1.jpg").writeBytes(ByteArray(2048))
        File(bookA, "2.jpg").writeBytes(ByteArray(1024))
        val bookB = File(root, "beta.zip").apply { writeBytes(ByteArray(4096)) }
        val ghost = File(root, "ghost").apply { mkdirs() }

        val books = listOf(
            book("alpha", bookA.absolutePath),
            book("Alpha", bookB.absolutePath), // duplicate title, different case
            book("ghost", ghost.absolutePath),
        )
        File(ghost, "1.jpg").writeBytes(ByteArray(16))
        val ghostRef = books[2]
        // The ghost book's directory disappears after scanning, like a user deleting it externally.
        ghost.deleteRecursively()

        val health = maintenance.audit(books)

        assertEquals(2048L + 1024L + 4096L, health.totalBytes)
        assertEquals(1, health.duplicateGroups.size)
        assertEquals(listOf("alpha", "Alpha"), health.duplicateGroups.single().map { it.title })
        assertEquals(listOf(ghostRef), health.missingBooks)
        assertFalse(health.isHealthy)
    }

    @Test
    fun `a clean library is healthy`() {
        val book = File(root, "solo").apply { mkdirs() }
        File(book, "1.jpg").writeBytes(ByteArray(512))

        val health = maintenance.audit(listOf(book("solo", book.absolutePath)))

        assertEquals(512L, health.totalBytes)
        assertTrue(health.duplicateGroups.isEmpty())
        assertTrue(health.missingBooks.isEmpty())
        assertTrue(health.isHealthy)
    }

    @Test
    fun `corrupt archives are flagged while valid ones pass`() {
        val goodZip = File(root, "good.zip")
        java.util.zip.ZipOutputStream(goodZip.outputStream()).use { it.putNextEntry(java.util.zip.ZipEntry("1.jpg")); it.closeEntry() }
        val badZip = File(root, "bad.zip").apply { writeBytes("definitely not a zip file".toByteArray()) }
        val bad7z = File(root, "bad.7z").apply { writeBytes(ByteArray(64) { 0x77 }) }
        val pdfBook = File(root, "text.txt").apply { writeBytes("小说正文".toByteArray()) }

        val health = maintenance.audit(
            listOf(
                book("good", goodZip.absolutePath).copy(format = LocalBookFormat.ZIP),
                book("bad", badZip.absolutePath).copy(format = LocalBookFormat.ZIP),
                book("bad7", bad7z.absolutePath).copy(format = LocalBookFormat.SEVEN_ZIP),
                book("novel", pdfBook.absolutePath).copy(format = LocalBookFormat.TXT),
            ),
        )

        assertEquals(listOf("bad", "bad7"), health.corruptBooks.map { it.title })
        assertEquals(0, health.missingBooks.size)
    }

    @Test
    fun `formats byte sizes`() {
        assertEquals("512 B", LocalLibraryMaintenance.formatBytes(512))
        assertEquals("2 KiB", LocalLibraryMaintenance.formatBytes(2048))
        assertEquals("1.0 MiB", LocalLibraryMaintenance.formatBytes(1024L * 1024))
        assertEquals("1.5 GiB", LocalLibraryMaintenance.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    private fun book(title: String, location: String) = LocalBook(
        id = location,
        title = title,
        format = LocalBookFormat.DIRECTORY,
        mediaType = MediaType.COMIC,
        location = location,
        lastModified = 0L,
    )
}
