package app.miyomu.reader.novel

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore

/**
 * A highlighted range inside one novel chapter (P1-7 高亮/批注最小切片). [startChar]/[endChar]
 * are chapter-relative character offsets into the chapter's plain text; [note] is optional
 * reader commentary.
 */
@Serializable
data class NovelHighlight(
    val chapterIndex: Int,
    val startChar: Int,
    val endChar: Int,
    val note: String = "",
    val createdAt: Long,
)

/**
 * Highlights for local novels, persisted as a JSON string in the shared [PreferenceStore]
 * instead of a Room table — mirrors [NovelBookmarkStore]: novel-only data, zero schema impact
 * on the comic side. Keys are per book location, so the store instance itself stays stateless.
 */
class NovelHighlightStore(
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

    fun bookKey(bookLocation: String): String = "novel_highlights_${bookLocation.hashCode()}"

    fun forChapter(bookLocation: String, chapterIndex: Int): List<NovelHighlight> =
        all(bookLocation).filter { it.chapterIndex == chapterIndex }

    fun all(bookLocation: String): List<NovelHighlight> =
        runCatching { json.decodeFromString<List<NovelHighlight>>(preference(bookLocation).get()) }
            .getOrDefault(emptyList())

    /** Replaces any highlight covering the same chapter range start. */
    fun add(bookLocation: String, highlight: NovelHighlight) {
        val rest = all(bookLocation)
            .filterNot { it.chapterIndex == highlight.chapterIndex && it.startChar == highlight.startChar }
        preference(bookLocation).set(json.encodeToString((rest + highlight).sortedBy { it.startChar }))
    }

    fun remove(bookLocation: String, highlight: NovelHighlight) {
        val rest = all(bookLocation).filterNot { it == highlight }
        preference(bookLocation).set(json.encodeToString(rest))
    }

    private companion object {
        const val EMPTY = "[]"
    }
}
