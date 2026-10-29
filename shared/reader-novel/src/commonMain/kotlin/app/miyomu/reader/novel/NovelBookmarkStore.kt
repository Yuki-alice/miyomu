package app.miyomu.reader.novel

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore

/**
 * A saved reading position inside one novel book. [excerpt] shows the text around the
 * position so the bookmark list reads like a quote collection.
 */
@Serializable
data class NovelBookmark(
    val chapterIndex: Int,
    val offset: Int,
    val excerpt: String,
    val createdAt: Long,
)

/**
 * Bookmarks for local novels, persisted as a JSON string in the shared [PreferenceStore]
 * instead of a Room table: novel-only data, zero schema impact on the comic side
 * (docs/11 §3). Keys are per book location, so the store instance itself stays stateless.
 */
class NovelBookmarkStore(
    private val store: PreferenceStore,
) {

    private val json = Json { ignoreUnknownKeys = true }

    // Preference instances are cached: the in-memory store used by tests/previews returns a
    // fresh snapshot per getString() call, so writes would be invisible without this.
    private val preferences = java.util.concurrent.ConcurrentHashMap<String, tachiyomi.core.common.preference.Preference<String>>()

    private fun preference(bookLocation: String): tachiyomi.core.common.preference.Preference<String> =
        preferences.getOrPut(bookKey(bookLocation)) {
            store.getString(bookKey(bookLocation), EMPTY)
        }

    fun bookKey(bookLocation: String): String = "novel_bookmarks_${bookLocation.hashCode()}"

    fun bookmarks(bookLocation: String): List<NovelBookmark> =
        decode(preference(bookLocation).get())

    /** Adds a bookmark unless an identical position already exists; returns the new list. */
    fun add(bookLocation: String, bookmark: NovelBookmark): List<NovelBookmark> {
        val current = bookmarks(bookLocation)
        if (current.any { it.chapterIndex == bookmark.chapterIndex && it.offset == bookmark.offset }) {
            return current
        }
        val next = (current + bookmark).sortedWith(
            compareBy({ it.chapterIndex }, { it.offset }),
        )
        write(bookLocation, next)
        return next
    }

    /** Removes the bookmark at [chapterIndex]/[offset]; returns the new list. */
    fun remove(bookLocation: String, chapterIndex: Int, offset: Int): List<NovelBookmark> {
        val next = bookmarks(bookLocation).filterNot {
            it.chapterIndex == chapterIndex && it.offset == offset
        }
        if (next.size != bookmarks(bookLocation).size) write(bookLocation, next)
        return next
    }

    /** True when the exact position already has a bookmark. */
    fun has(bookLocation: String, chapterIndex: Int, offset: Int): Boolean =
        bookmarks(bookLocation).any { it.chapterIndex == chapterIndex && it.offset == offset }

    private fun write(bookLocation: String, bookmarks: List<NovelBookmark>) {
        preference(bookLocation).set(json.encodeToString(bookmarks))
    }

    private fun decode(raw: String): List<NovelBookmark> =
        runCatching { json.decodeFromString<List<NovelBookmark>>(raw) }.getOrDefault(emptyList())

    companion object {
        private const val EMPTY = "[]"
    }
}
