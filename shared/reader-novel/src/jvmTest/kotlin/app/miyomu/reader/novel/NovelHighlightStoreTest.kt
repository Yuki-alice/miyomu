package app.miyomu.reader.novel

import tachiyomi.core.common.preference.InMemoryPreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** P1-7 高亮: per-chapter listing, upsert-by-start, removal, ordering and corrupt degradation. */
class NovelHighlightStoreTest {

    private val location = "/lib/novel.txt"

    private fun store() = NovelHighlightStore(InMemoryPreferenceStore())

    @Test
    fun `adds and lists highlights per chapter`() {
        val highlights = store()
        highlights.add(location, NovelHighlight(0, 10, 40, "开篇伏笔", createdAt = 1))
        highlights.add(location, NovelHighlight(2, 0, 30, "", createdAt = 2))

        assertEquals(listOf(10), highlights.forChapter(location, 0).map { it.startChar })
        assertEquals(listOf(0), highlights.forChapter(location, 2).map { it.startChar })
        assertEquals(0, highlights.forChapter(location, 1).size)
        assertEquals(2, highlights.all(location).size)
    }

    @Test
    fun `same chapter start replaces the previous highlight`() {
        val highlights = store()
        highlights.add(location, NovelHighlight(0, 10, 40, "旧批注", createdAt = 1))
        highlights.add(location, NovelHighlight(0, 10, 60, "新批注", createdAt = 2))

        val chapter = highlights.forChapter(location, 0)
        assertEquals(1, chapter.size)
        assertEquals("新批注", chapter.single().note)
        assertEquals(60, chapter.single().endChar)
    }

    @Test
    fun `remove drops exactly the target highlight`() {
        val highlights = store()
        val first = NovelHighlight(0, 10, 40, "一", createdAt = 1)
        val second = NovelHighlight(0, 100, 140, "二", createdAt = 2)
        highlights.add(location, first)
        highlights.add(location, second)

        highlights.remove(location, first)

        assertEquals(listOf(second), highlights.forChapter(location, 0))
    }

    @Test
    fun `kept in chapter order by start char`() {
        val highlights = store()
        highlights.add(location, NovelHighlight(0, 200, 240, "", createdAt = 1))
        highlights.add(location, NovelHighlight(0, 5, 40, "", createdAt = 2))

        assertEquals(listOf(5, 200), highlights.forChapter(location, 0).map { it.startChar })
    }

    @Test
    fun `corrupt storage degrades to empty`() {
        val memory = InMemoryPreferenceStore()
        val store = NovelHighlightStore(memory)
        memory.getString(store.bookKey(location), "[]").set("not-json")

        assertTrue(store.all(location).isEmpty())
        assertEquals(0, store.forChapter(location, 0).size)
    }
}
