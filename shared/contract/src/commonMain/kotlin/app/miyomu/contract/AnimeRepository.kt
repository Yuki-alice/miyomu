package app.miyomu.contract

import app.miyomu.model.ResolvedVideo
import kotlinx.coroutines.flow.Flow

/** Data-layer record for one episode of an anime item; `rowId`/marks come from the store. */
data class EpisodeRecord(
    val key: String,
    val name: String,
    val url: String,
    val episodeNumber: Double? = null,
    val sourceOrder: Long = 0L,
    val seen: Boolean = false,
    val bookmark: Boolean = false,
    val fetchedAt: Long = 0L,
)

interface AnimeRepository {
    fun observeEpisodes(itemId: String): Flow<List<EpisodeRecord>>
    suspend fun episodes(itemId: String): List<EpisodeRecord>

    /** Removes every episode row incl. seen/bookmark marks (data settings). */
    suspend fun clearAll()

    /**
     * Upserts `episodes` and deletes stored keys not present in the list. Marks (seen/bookmark)
     * of surviving keys are preserved, so a refresh never wipes user state.
     */
    suspend fun replaceEpisodes(itemId: String, episodes: List<EpisodeRecord>, fetchedAt: Long)
    suspend fun setSeen(itemId: String, keys: List<String>, seen: Boolean)
    suspend fun setBookmarked(itemId: String, keys: List<String>, bookmarked: Boolean)
    suspend fun clear(itemId: String)
}

/** Data-layer record for one chapter of a comic item. */
data class ChapterRecord(
    val key: String,
    val name: String,
    val url: String,
    val chapterNumber: Double? = null,
    val sourceOrder: Long = 0L,
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val fetchedAt: Long = 0L,
)

interface ChapterRepository {
    fun observeChapters(itemId: String): Flow<List<ChapterRecord>>
    suspend fun chapters(itemId: String): List<ChapterRecord>
    suspend fun replaceChapters(itemId: String, chapters: List<ChapterRecord>, fetchedAt: Long)
    suspend fun setRead(itemId: String, keys: List<String>, read: Boolean)
    suspend fun setBookmarked(itemId: String, keys: List<String>, bookmarked: Boolean)
    suspend fun clear(itemId: String)

    /** Removes every chapter row incl. read/bookmark marks (data settings). */
    suspend fun clearAll()
}

interface VideoResolveRepository {
    /** Returns the cached resolve for `key` when younger than `maxAgeMillis`, else null. */
    suspend fun get(key: String, maxAgeMillis: Long, now: Long): ResolvedVideo?
    suspend fun put(key: String, video: ResolvedVideo, now: Long)
    suspend fun clearExpired(now: Long, maxAgeMillis: Long)

    /** Removes every cached resolve (data settings). */
    suspend fun clearAll()
}

