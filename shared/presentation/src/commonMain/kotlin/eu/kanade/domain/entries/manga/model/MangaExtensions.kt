package eu.kanade.domain.entries.manga.model

import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entries.manga.model.Manga

/** CMP port: flag-based Manga extensions kept; Injekt/cover-cache helpers deferred. */

/** Local manga sources have id 0 in Tachiyomi's source-local module. */
const val LOCAL_MANGA_SOURCE_ID = 0L

fun Manga.isLocal(): Boolean = source == LOCAL_MANGA_SOURCE_ID

/**
 * CMP port: the `downloadedOnly` global override (BasePreferences) is not applied yet;
 * the filter follows the per-manga chapter flags only.
 */
val Manga.downloadedFilter: TriState
    get() = when (downloadedFilterRaw) {
        Manga.CHAPTER_SHOW_DOWNLOADED -> TriState.ENABLED_IS
        Manga.CHAPTER_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

fun Manga.chaptersFiltered(): Boolean {
    return unreadFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED
}
