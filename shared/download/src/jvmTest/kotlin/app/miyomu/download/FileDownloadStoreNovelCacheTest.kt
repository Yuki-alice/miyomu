package app.miyomu.download

import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** P1-2 contract: the download store doubles as the offline novel chapter cache. */
class FileDownloadStoreNovelCacheTest {

    @TempDir
    lateinit var root: File

    private fun store(): FileDownloadStore = FileDownloadStore(root)

    @Test
    fun `writes, reads and overwrites novel chapters`() {
        val store = store()
        assertFalse(store.isCached("legado:src:book", "https://s.example/c1"))
        assertNull(store.read("legado:src:book", "https://s.example/c1"))

        store.write("legado:src:book", "https://s.example/c1", "第一段正文。")
        assertTrue(store.isCached("legado:src:book", "https://s.example/c1"))
        assertEquals("第一段正文。", store.read("legado:src:book", "https://s.example/c1"))

        store.write("legado:src:book", "https://s.example/c1", "更新后的正文。")
        assertEquals("更新后的正文。", store.read("legado:src:book", "https://s.example/c1"))
    }

    @Test
    fun `keys isolate books and chapters`() {
        val store = store()
        store.write("legado:src:book", "https://s.example/c1", "第一章")
        store.write("legado:src:book", "https://s.example/c2", "第二章")
        store.write("legado:src:other", "https://s.example/c1", "另一本书")

        assertEquals("第二章", store.read("legado:src:book", "https://s.example/c2"))
        assertEquals("另一本书", store.read("legado:src:other", "https://s.example/c1"))
    }

    @Test
    fun `deleteNovelBook drops the whole shelf entry`() {
        val store = store()
        store.write("legado:src:book", "https://s.example/c1", "第一章")
        store.write("legado:src:book", "https://s.example/c2", "第二章")
        store.write("legado:src:other", "https://s.example/c1", "另一本书")

        store.deleteNovelBook("legado:src:book")

        assertFalse(store.isCached("legado:src:book", "https://s.example/c1"))
        assertFalse(store.isCached("legado:src:book", "https://s.example/c2"))
        assertTrue(store.isCached("legado:src:other", "https://s.example/c1"))
    }
}
