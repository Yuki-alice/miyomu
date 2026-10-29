package app.miyomu.novel

import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalFileRef
import app.miyomu.model.MediaType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NovelLibraryScannerTest {

    private class FakeRef(
        override val name: String,
        override val location: String = name,
        override val isDirectory: Boolean = false,
        override val size: Long = 0L,
        override val lastModified: Long = 0L,
        private val children: List<FakeRef> = emptyList(),
    ) : LocalFileRef {
        override fun list(): List<LocalFileRef> = children
        override fun readBytes(): ByteArray = ByteArray(0)
    }

    private val scanner = NovelLibraryScanner()

    @Test
    fun `claims only txt and epub entries with novel media type`() {
        val root = FakeRef(
            "root",
            isDirectory = true,
            children = listOf(
                FakeRef("小说b.txt"),
                FakeRef("novel2.epub"),
                FakeRef("manga.zip"),
                FakeRef("image.jpg"),
                FakeRef(".hidden.txt"),
                FakeRef("cover", isDirectory = true),
            ),
        )

        val books = scanner.scan(root)

        assertEquals(listOf("novel2", "小说b"), books.map { it.title })
        assertEquals(listOf(LocalBookFormat.EPUB, LocalBookFormat.TXT), books.map { it.format })
        assertTrue(books.all { it.mediaType == MediaType.NOVEL })
        assertTrue(books.all { it.id == it.location })
    }

    @Test
    fun `single file root is one book`() {
        val books = scanner.scan(FakeRef("solo.txt"))

        assertEquals(1, books.size)
        assertEquals("solo", books[0].title)
    }

    @Test
    fun `sorts naturally so chapter-like numbers stay in order`() {
        val root = FakeRef(
            "root",
            isDirectory = true,
            children = listOf(
                FakeRef("卷 10.txt"),
                FakeRef("卷 2.txt"),
                FakeRef("卷 1.txt"),
            ),
        )

        assertEquals(listOf("卷 1", "卷 2", "卷 10"), scanner.scan(root).map { it.title })
    }
}
