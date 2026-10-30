package app.miyomu.source.novel

import app.miyomu.domain.preferences.JsonPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegadoSourceStoreTest {

    @TempDir
    lateinit var tempDir: Path

    private fun store() = LegadoSourceStore(
        // Unconfined scope makes JSON writes synchronous for deterministic assertions.
        JsonPreferenceStore(tempDir.resolve("prefs.json").toFile(), CoroutineScope(Dispatchers.Unconfined)),
    )

    private val sourceA = LegadoBookSource.fromJson("""{"bookSourceUrl":"https://a.example","bookSourceName":"源A"}""")
    private val sourceB = LegadoBookSource.fromJson("""{"bookSourceUrl":"https://b.example","bookSourceName":"源B"}""")

    @Test
    fun `sources dedupe by url and persist across instances`() {
        val first = store()
        first.addSource(sourceA)
        first.addSource(sourceA)
        first.addSource(sourceB)

        assertEquals(listOf("源A", "源B"), store().sources().map { it.bookSourceName })

        first.removeSource("https://a.example")
        assertEquals(listOf("源B"), store().sources().map { it.bookSourceName })
    }

    @Test
    fun `shelf adds by book url and persists`() {
        val first = store()
        val book = LegadoOnlineBook("书一", "作者", "https://s.example/b/1", "https://s.example/b/1/toc", null, "https://s.example")
        first.addToShelf(book)
        first.addToShelf(book.copy(coverUrl = "/c.jpg")) // re-add updates instead of duplicating

        val second = store()
        val shelf = second.shelf()
        assertEquals(1, shelf.size)
        assertEquals("书一", shelf[0].name)
        assertEquals("/c.jpg", shelf[0].coverUrl)
        assertTrue(second.inShelf("https://s.example/b/1"))

        second.removeFromShelf("https://s.example/b/1")
        assertFalse(second.inShelf("https://s.example/b/1"))
    }
}
