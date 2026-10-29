package eu.kanade.domain.entries.anime.model

import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entries.anime.model.Anime

/** CMP port: flag-based Anime extensions kept; Injekt/BasePreferences overrides deferred. */

/** Local anime sources have id 0, matching Tachiyomi's source-local module. */
const val LOCAL_ANIME_SOURCE_ID = 0L

fun Anime.isLocal(): Boolean = source == LOCAL_ANIME_SOURCE_ID

val Anime.downloadedFilter: TriState
    get() = when (downloadedFilterRaw) {
        Anime.EPISODE_SHOW_DOWNLOADED -> TriState.ENABLED_IS
        Anime.EPISODE_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

val Anime.seasonDownloadedFilter: TriState
    get() = when (seasonDownloadedFilterRaw) {
        Anime.SEASON_SHOW_DOWNLOADED -> TriState.ENABLED_IS
        Anime.SEASON_SHOW_NOT_DOWNLOADED -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }

fun Anime.episodesFiltered(): Boolean {
    return unseenFilter != TriState.DISABLED ||
        downloadedFilter != TriState.DISABLED ||
        bookmarkedFilter != TriState.DISABLED ||
        fillermarkedFilter != TriState.DISABLED
}

fun Anime.seasonsFiltered(): Boolean {
    return seasonDownloadedFilter != TriState.DISABLED ||
        seasonUnseenFilter != TriState.DISABLED ||
        seasonStartedFilter != TriState.DISABLED ||
        seasonCompletedFilter != TriState.DISABLED ||
        seasonBookmarkedFilter != TriState.DISABLED ||
        seasonFillermarkedFilter != TriState.DISABLED
}
