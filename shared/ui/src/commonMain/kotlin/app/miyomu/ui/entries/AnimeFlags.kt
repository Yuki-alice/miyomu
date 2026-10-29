package app.miyomu.ui.entries

import aniyomi.domain.anime.SeasonDisplayMode
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entries.anime.model.Anime

/** Default episode list for local video: ascending by parsed episode number. */
internal val DefaultVideoEpisodeFlags: Long =
    Anime.EPISODE_SORTING_NUMBER or Anime.EPISODE_SORT_ASC

/** Default season list: unseen/local badges plus the continue-watching overlay. */
internal val DefaultVideoSeasonFlags: Long =
    Anime.SEASON_OVERLAY_UNSEEN_MASK or
        Anime.SEASON_OVERLAY_LOCAL_MASK or
        Anime.SEASON_OVERLAY_CONT_MASK

private fun Long.withBits(mask: Long, value: Long): Long = (this and mask.inv()) or (value and mask)

internal fun Long.withEpisodeUnseenFilter(state: TriState): Long = withBits(
    Anime.EPISODE_UNSEEN_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.EPISODE_SHOW_UNSEEN
        TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_SEEN
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withEpisodeDownloadedFilter(state: TriState): Long = withBits(
    Anime.EPISODE_DOWNLOADED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.EPISODE_SHOW_DOWNLOADED
        TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_DOWNLOADED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withEpisodeBookmarkedFilter(state: TriState): Long = withBits(
    Anime.EPISODE_BOOKMARKED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.EPISODE_SHOW_BOOKMARKED
        TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_BOOKMARKED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withEpisodeFillermarkedFilter(state: TriState): Long = withBits(
    Anime.EPISODE_FILLERMARKED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.EPISODE_SHOW_FILLERMARKED
        TriState.ENABLED_NOT -> Anime.EPISODE_SHOW_NOT_FILLERMARKED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withEpisodeSorting(mode: Long): Long = withBits(Anime.EPISODE_SORTING_MASK, mode)

internal fun Long.withEpisodeDisplayMode(mode: Long): Long =
    withBits(Anime.EPISODE_DISPLAY_MASK, mode)

internal fun Long.withEpisodePreviews(flag: Long): Long =
    withBits(Anime.EPISODE_PREVIEWS_MASK, flag)

internal fun Long.withEpisodeSummaries(flag: Long): Long =
    withBits(Anime.EPISODE_SUMMARIES_MASK, flag)

internal fun Long.withSeasonDownloadedFilter(state: TriState): Long = withBits(
    Anime.SEASON_DOWNLOADED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.SEASON_SHOW_DOWNLOADED
        TriState.ENABLED_NOT -> Anime.SEASON_SHOW_NOT_DOWNLOADED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withSeasonUnseenFilter(state: TriState): Long = withBits(
    Anime.SEASON_UNSEEN_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.SEASON_SHOW_UNSEEN
        TriState.ENABLED_NOT -> Anime.SEASON_SHOW_SEEN
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withSeasonStartedFilter(state: TriState): Long = withBits(
    Anime.SEASON_STARTED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.SEASON_SHOW_STARTED
        TriState.ENABLED_NOT -> Anime.SEASON_SHOW_NOT_STARTED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withSeasonCompletedFilter(state: TriState): Long = withBits(
    Anime.SEASON_COMPLETED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.SEASON_SHOW_COMPLETED
        TriState.ENABLED_NOT -> Anime.SEASON_SHOW_NOT_COMPLETED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withSeasonBookmarkedFilter(state: TriState): Long = withBits(
    Anime.SEASON_BOOKMARKED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.SEASON_SHOW_BOOKMARKED
        TriState.ENABLED_NOT -> Anime.SEASON_SHOW_NOT_BOOKMARKED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withSeasonFillermarkedFilter(state: TriState): Long = withBits(
    Anime.SEASON_FILLERMARKED_MASK,
    when (state) {
        TriState.ENABLED_IS -> Anime.SEASON_SHOW_FILLERMARKED
        TriState.ENABLED_NOT -> Anime.SEASON_SHOW_NOT_FILLERMARKED
        TriState.DISABLED -> 0L
    },
)

internal fun Long.withSeasonSorting(mode: Long): Long = withBits(Anime.SEASON_SORT_MASK, mode)

internal fun Long.withSeasonGridMode(mode: SeasonDisplayMode): Long = withBits(
    Anime.SEASON_GRID_DISPLAY_MODE_MASK,
    SeasonDisplayMode.toLong(mode) shl Anime.SEASON_GRID_DISPLAY_MODE_BIT_OFFSET,
)

internal fun Long.withSeasonGridSize(size: Int): Long = withBits(
    Anime.SEASON_GRID_DISPLAY_SIZE_MASK,
    size.toLong() shl Anime.SEASON_GRID_DISPLAY_SIZE_BIT_OFFSET,
)

internal fun Long.withSeasonOverlayDownloaded(enabled: Boolean): Long =
    withBits(Anime.SEASON_OVERLAY_DOWNLOADED_MASK, if (enabled) Anime.SEASON_OVERLAY_DOWNLOADED_MASK else 0L)

internal fun Long.withSeasonOverlayUnseen(enabled: Boolean): Long =
    withBits(Anime.SEASON_OVERLAY_UNSEEN_MASK, if (enabled) Anime.SEASON_OVERLAY_UNSEEN_MASK else 0L)

internal fun Long.withSeasonOverlayLocal(enabled: Boolean): Long =
    withBits(Anime.SEASON_OVERLAY_LOCAL_MASK, if (enabled) Anime.SEASON_OVERLAY_LOCAL_MASK else 0L)

internal fun Long.withSeasonOverlayLang(enabled: Boolean): Long =
    withBits(Anime.SEASON_OVERLAY_LANG_MASK, if (enabled) Anime.SEASON_OVERLAY_LANG_MASK else 0L)

internal fun Long.withSeasonOverlayContinue(enabled: Boolean): Long =
    withBits(Anime.SEASON_OVERLAY_CONT_MASK, if (enabled) Anime.SEASON_OVERLAY_CONT_MASK else 0L)

internal fun Long.withSeasonDisplayMode(mode: Long): Long =
    withBits(Anime.SEASON_DISPLAY_MODE_MASK, mode)
