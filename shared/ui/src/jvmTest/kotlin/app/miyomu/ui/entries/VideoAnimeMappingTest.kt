package app.miyomu.ui.entries

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.entries.anime.model.Anime

class VideoAnimeMappingTest {

    private val book = LocalBook(
        id = "/library/test.mkv",
        title = "test",
        format = LocalBookFormat.VIDEO,
        mediaType = MediaType.VIDEO,
        location = "/library/test.mkv",
        lastModified = 1_700_000_000_000L,
    )

    @Test
    fun `video maps to a local single-episode anime`() {
        val anime = book.toAnime()

        assertEquals(book.animeId(), anime.id)
        assertEquals(0L, anime.source)
        assertTrue(anime.favorite)
        assertEquals("test", anime.title)
        assertEquals(book.location, anime.url)
        assertEquals(1_700_000_000_000L, anime.lastModifiedAt)
        assertEquals(Anime.create().fetchType, anime.fetchType)
    }

    @Test
    fun `ids stay stable across rescans`() {
        val rescanned = book.copy(lastModified = 1_800_000_000_000L)

        assertEquals(book.animeId(), rescanned.animeId())
        assertEquals(book.episodeId(), rescanned.episodeId())
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

        val episode = book.toEpisode(progress)

        assertEquals(book.episodeId(), episode.id)
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

        assertTrue(book.toEpisode(progress).seen)
    }
}
