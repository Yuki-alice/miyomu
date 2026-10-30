package app.miyomu.ui

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.model.MediaType
import java.io.File
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** P1-1 封面候选: candidate enumeration, override save/resolve and the missing-file path. */
class LocalCoverOverrideTest {

    @TempDir
    lateinit var root: File

    private val store = InMemoryPreferenceStore()

    private fun dirBook(dir: File) = LocalBook(
        id = dir.absolutePath,
        title = dir.name,
        format = LocalBookFormat.DIRECTORY,
        mediaType = MediaType.COMIC,
        location = dir.absolutePath,
        lastModified = 0L,
    )

    @Test
    fun `lists top-level images as candidates for directory books`() {
        val bookDir = File(root, "book").apply { mkdirs() }
        val cover = File(bookDir, "alt-cover.PNG").apply { writeBytes(byteArrayOf(1)) }
        File(bookDir, "chapter1").apply { mkdirs() }
        File(bookDir, "notes.txt").apply { writeText("x") }

        val candidates = localCoverCandidates(dirBook(bookDir))

        assertEquals(listOf(cover), candidates)
    }

    @Test
    fun `single-file books have no candidates`() {
        val archive = File(root, "book.cbz").apply { writeBytes(byteArrayOf(1)) }

        assertEquals(emptyList(), localCoverCandidates(dirBook(archive).copy(format = LocalBookFormat.ZIP)))
    }

    @Test
    fun `override resolves the picked file`() {
        val bookDir = File(root, "book").apply { mkdirs() }
        val candidate = File(bookDir, "alt.jpg").apply { writeBytes(byteArrayOf(7, 8)) }
        val book = dirBook(bookDir)

        saveCoverOverride(store, book, candidate)

        val bytes = localCoverOverrideBytes(store, book)
        assertNotNull(bytes)
        assertEquals(2, bytes.size)
    }

    @Test
    fun `missing override file degrades to null`() {
        val bookDir = File(root, "book").apply { mkdirs() }
        val book = dirBook(bookDir)
        // Stale override pointing at a file that no longer exists.
        saveCoverOverride(store, book, File(bookDir, "deleted.jpg"))
        File(bookDir, "deleted.jpg").delete()

        assertNull(localCoverOverrideBytes(store, book))
    }

    @Test
    fun `unset override returns null`() {
        val bookDir = File(root, "book").apply { mkdirs() }

        assertNull(localCoverOverrideBytes(store, dirBook(bookDir)))
    }
}
