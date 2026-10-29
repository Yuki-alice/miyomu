package app.miyomu.ui.entries

import aniyomi.domain.anime.SeasonDisplayMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entries.anime.model.Anime

class AnimeFlagsTest {

    @Test
    fun `episode filters replace only their own bits`() {
        val flags = DefaultVideoEpisodeFlags

        val unseen = flags.withEpisodeUnseenFilter(TriState.ENABLED_IS)
        assertEquals(Anime.EPISODE_SHOW_UNSEEN, unseen and Anime.EPISODE_UNSEEN_MASK)
        assertEquals(Anime.EPISODE_SORTING_NUMBER, unseen and Anime.EPISODE_SORTING_MASK)

        val bookmarked = unseen.withEpisodeBookmarkedFilter(TriState.ENABLED_NOT)
        assertEquals(Anime.EPISODE_SHOW_NOT_BOOKMARKED, bookmarked and Anime.EPISODE_BOOKMARKED_MASK)
        assertEquals(Anime.EPISODE_SHOW_UNSEEN, bookmarked and Anime.EPISODE_UNSEEN_MASK)

        assertEquals(0L, bookmarked.withEpisodeUnseenFilter(TriState.DISABLED) and Anime.EPISODE_UNSEEN_MASK)
    }

    @Test
    fun `episode sorting and display keep other flags`() {
        val flags = DefaultVideoEpisodeFlags
            .withEpisodeUnseenFilter(TriState.ENABLED_IS)
            .withEpisodeDisplayMode(Anime.EPISODE_DISPLAY_NUMBER)
            .withEpisodePreviews(Anime.EPISODE_SHOW_NOT_PREVIEWS)

        assertEquals(Anime.EPISODE_DISPLAY_NUMBER, flags and Anime.EPISODE_DISPLAY_MASK)
        assertEquals(Anime.EPISODE_SHOW_NOT_PREVIEWS, flags and Anime.EPISODE_PREVIEWS_MASK)
        assertEquals(Anime.EPISODE_SORT_ASC, flags and Anime.EPISODE_SORT_DIR_MASK)

        val reordered = flags.withEpisodeSorting(Anime.EPISODE_SORTING_UPLOAD_DATE)
        assertEquals(Anime.EPISODE_SORTING_UPLOAD_DATE, reordered and Anime.EPISODE_SORTING_MASK)
        assertEquals(Anime.EPISODE_SHOW_UNSEEN, reordered and Anime.EPISODE_UNSEEN_MASK)
    }

    @Test
    fun `season grid mode size and overlays round-trip`() {
        var flags = DefaultVideoSeasonFlags

        assertEquals(SeasonDisplayMode.CompactGrid, seasonGridModeOf(flags))
        flags = flags.withSeasonGridMode(SeasonDisplayMode.List)
        assertEquals(SeasonDisplayMode.List, seasonGridModeOf(flags))

        flags = flags.withSeasonGridSize(4)
        assertEquals(4, ((flags and Anime.SEASON_GRID_DISPLAY_SIZE_MASK) shr
            Anime.SEASON_GRID_DISPLAY_SIZE_BIT_OFFSET).toInt())

        flags = flags.withSeasonOverlayDownloaded(true).withSeasonOverlayLang(true)
        assertTrue(flags and Anime.SEASON_OVERLAY_DOWNLOADED_MASK != 0L)
        assertTrue(flags and Anime.SEASON_OVERLAY_LANG_MASK != 0L)
        assertFalse(flags.withSeasonOverlayUnseen(false) and Anime.SEASON_OVERLAY_UNSEEN_MASK != 0L)
        assertFalse(flags.withSeasonOverlayContinue(false) and Anime.SEASON_OVERLAY_CONT_MASK != 0L)
    }

    private fun seasonGridModeOf(flags: Long): SeasonDisplayMode = SeasonDisplayMode.fromLong(
        (flags and Anime.SEASON_GRID_DISPLAY_MODE_MASK) shr Anime.SEASON_GRID_DISPLAY_MODE_BIT_OFFSET,
    )
}
