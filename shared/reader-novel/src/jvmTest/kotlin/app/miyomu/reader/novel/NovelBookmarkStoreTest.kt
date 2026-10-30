package app.miyomu.reader.novel

import app.miyomu.domain.preferences.JsonPreferenceStore
import app.miyomu.novel.NovelChapter
import app.miyomu.novel.ParsedNovel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NovelBookmarkStoreTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `adds sorted bookmarks and deduplicates positions`() {
        val store = NovelBookmarkStore(InMemoryPreferenceStore())
        val book = "loc-A"

        store.add(book, NovelBookmark(2, 100, "二", 3L))
        store.add(book, NovelBookmark(0, 50, "一", 1L))
        val duplicated = store.add(book, NovelBookmark(2, 100, "二", 4L))

        assertEquals(
            listOf(
                NovelBookmark(0, 50, "一", 1L),
                NovelBookmark(2, 100, "二", 3L),
            ),
            duplicated,
        )
        assertTrue(store.has(book, 0, 50))
        assertTrue(store.has(book, 2, 100))
        assertFalse(store.has(book, 1, 0))
    }

    @Test
    fun `removes bookmarks and persists across store instances`() {
        val file = tempDir.resolve("prefs.json").toFile()
        // Unconfined scope makes the JSON file write synchronous, so reopening the store
        // below deterministically sees the persisted values (production writes async).
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val book = "loc-B"
        val first = NovelBookmarkStore(JsonPreferenceStore(file, scope))
        first.add(book, NovelBookmark(1, 10, "甲", 1L))
        first.add(book, NovelBookmark(3, 20, "乙", 2L))

        val second = NovelBookmarkStore(JsonPreferenceStore(file, scope))
        second.remove(book, 1, 10)

        val remaining = NovelBookmarkStore(JsonPreferenceStore(file, scope)).bookmarks(book)
        assertEquals(listOf(NovelBookmark(3, 20, "乙", 2L)), remaining)
        // Different books never share bookmarks.
        assertTrue(second.bookmarks("loc-C").isEmpty())
    }

    @Test
    fun `corrupt json decodes to empty`() {
        val file = tempDir.resolve("broken.json").toFile()
        file.writeText("{not json")

        assertTrue(NovelBookmarkStore(JsonPreferenceStore(file)).bookmarks("loc").isEmpty())
    }
}

class NovelBookSearchTest {

    private fun chapter(title: String?, text: String): NovelChapter = object : NovelChapter {
        override val title: String? = title
        override fun text(): String = text
    }

    @Test
    fun `finds title and body matches with excerpts`() {
        val novel = ParsedNovel(
            title = "t",
            chapters = listOf(
                chapter("第一章 起点", "山谷里有一条小路。"),
                chapter("第二章 小路", "小路的尽头是一片海。"),
            ),
        )

        val matches = NovelBookSearch.search(novel.chapters, "小路")

        // Chapter order first: ch0 body hit, then ch1 title hit, then ch1 body hit.
        assertEquals(3, matches.size)
        assertFalse(matches[0].inTitle)
        assertEquals(0, matches[0].chapterIndex)
        assertEquals(6, matches[0].offset)
        assertTrue(matches[1].inTitle)
        assertEquals(1, matches[1].chapterIndex)
        assertEquals(0, matches[2].offset)
        assertTrue(matches[2].excerpt.contains("小路"))
    }

    @Test
    fun `search is case insensitive and capped`() {
        val novel = ParsedNovel(
            title = "t",
            chapters = listOf(chapter(null, "Java java JAVA ".repeat(30))),
        )

        val matches = NovelBookSearch.search(novel.chapters, "java", maxResults = 7)

        assertEquals(7, matches.size)
        assertTrue(matches.all { !it.inTitle })
    }

    @Test
    fun `blank query returns nothing`() {
        val novel = ParsedNovel(title = "t", chapters = listOf(chapter("x", "abc")))
        assertTrue(NovelBookSearch.search(novel.chapters, "  ").isEmpty())
    }
}
