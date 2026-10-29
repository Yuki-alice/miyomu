package app.miyomu.ui.entries

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalChapter
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import eu.kanade.tachiyomi.animesource.model.FetchType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoAnimeMappingTest {

    private val book = LocalBook(
        id = "/library/test.mkv",
        title = "test",
        format = LocalBookFormat.VIDEO,
        mediaType = MediaType.VIDEO,
        location = "/library/test.mkv",
        lastModified = 1_700_000_000_000L,
    )

    private val chapter = LocalChapter(
        id = "/library/test.mkv",
        title = "test",
        bookLocation = book.location,
        location = book.location,
        format = LocalBookFormat.VIDEO,
        lastModified = 1_700_000_000_000L,
    )

    @Test
    fun `video maps to a local anime`() {
        val anime = book.toAnime()

        assertEquals(book.animeId(), anime.id)
        assertEquals(0L, anime.source)
        assertTrue(anime.favorite)
        assertEquals("test", anime.title)
        assertEquals(book.location, anime.url)
        assertEquals(1_700_000_000_000L, anime.lastModifiedAt)
        assertEquals(FetchType.Episodes, anime.fetchType)
        assertEquals(FetchType.Seasons, book.toAnime(FetchType.Seasons).fetchType)
    }

    @Test
    fun `ids stay stable across rescans`() {
        val rescanned = book.copy(lastModified = 1_800_000_000_000L)

        assertEquals(book.animeId(), rescanned.animeId())
        assertEquals(chapter.episodeId(), chapter.copy(lastModified = 1L).episodeId())
        assertEquals(book.toAnime().id, rescanned.toAnime().id)
    }

    @Test
    fun `saved timestamp becomes the episode position`() {
        val progress = ReadingProgress(
            itemId = "local:/library/test.mkv",
            entryId = book.location,
            kind = ProgressKind.TIMESTAMP,
            position = 19_608.0,
            duration = 30_023.0,
            updatedAt = 1_000L,
            deviceId = "test",
        )

        val episode = chapter.toEpisode(book, progress, index = 0)

        assertEquals(chapter.episodeId(), episode.id)
        assertEquals(book.animeId(), episode.animeId)
        assertEquals(19_608L, episode.lastSecondSeen)
        assertEquals(30_023L, episode.totalSeconds)
        assertFalse(episode.seen)
    }

    @Test
    fun `finishing an episode marks it seen`() {
        val progress = ReadingProgress(
            itemId = "local:/library/test.mkv",
            entryId = book.location,
            kind = ProgressKind.TIMESTAMP,
            position = 29_500.0,
            duration = 30_023.0,
            updatedAt = 1_000L,
            deviceId = "test",
        )

        assertTrue(chapter.toEpisode(book, progress, index = 0).seen)
        assertTrue(progress.isWatched())
        assertFalse((null as ReadingProgress?).isWatched())
    }

    @Test
    fun `episode numbers are parsed from file names`() {
        fun episodeNumberOf(name: String) = chapter
            .copy(title = name, location = "/library/$name.mkv")
            .toEpisode(book, progress = null, index = 0)
            .episodeNumber

        assertEquals(2.0, episodeNumberOf("Show S01E02"))
        assertEquals(12.0, episodeNumberOf("Show - 12"))
        assertEquals(-1.0, episodeNumberOf("Show"))
    }

    @Test
    fun `season item counts watched episodes`() {
        val season = book.copy(
            id = "/library/Season 1",
            title = "Season 1",
            location = "/library/Season 1",
        )
        val episodes = listOf(
            chapter.copy(location = "/library/Season 1/E01.mkv"),
            chapter.copy(location = "/library/Season 1/E02.mkv"),
        )
        val progress = listOf(
            ReadingProgress(
                itemId = "local:/library/Season 1",
                entryId = "/library/Season 1/E01.mkv",
                kind = ProgressKind.TIMESTAMP,
                position = 1_400.0,
                duration = 1_440.0,
                updatedAt = 5_000L,
                deviceId = "test",
            ),
        )

        val item = season.toSeasonItem(episodes, progress)

        assertEquals("Season 1", item.seasonAnime.anime.title)
        assertEquals(2L, item.seasonAnime.totalCount)
        assertEquals(1L, item.seasonAnime.seenCount)
        assertEquals(5_000L, item.seasonAnime.lastSeen)
    }
}
