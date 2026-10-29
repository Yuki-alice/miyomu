package app.miyomu.ui.entries

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalChapter
import app.miyomu.model.ReadingProgress
import aniyomi.domain.anime.SeasonAnime
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.ui.entries.anime.AnimeSeasonItem
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode
import tachiyomi.domain.items.episode.service.EpisodeRecognition

/** Matches Aniyomi's default `progressPreference` for marking an episode watched. */
private const val WATCHED_FRACTION = 0.85

/** Stable anime id derived from the on-disk location, matching the manga/library mapping. */
internal fun LocalBook.animeId(): Long = id.hashCode().toLong() and 0xFFFFFFFFL

/** Stable episode id derived from the on-disk location. */
internal fun LocalChapter.episodeId(): Long = location.hashCode().toLong() and 0xFFFFFFFFL

/**
 * A local video file or a folder of episodes is an anime. Folders whose children are folders
 * of video files use [FetchType.Seasons]; the children become season books of their own.
 */
internal fun LocalBook.toAnime(fetchType: FetchType = FetchType.Episodes): Anime = Anime.create().copy(
    id = animeId(),
    source = 0L,
    favorite = true,
    lastUpdate = lastModified,
    dateAdded = lastModified,
    url = location,
    title = title,
    status = SAnime.UNKNOWN.toLong(),
    episodeFlags = DefaultVideoEpisodeFlags,
    seasonFlags = DefaultVideoSeasonFlags,
    initialized = true,
    lastModifiedAt = lastModified,
    version = 1L,
    fetchType = fetchType,
)

/** A saved position counts as watched past Aniyomi's default threshold. */
internal fun ReadingProgress?.isWatched(): Boolean =
    this?.duration?.let { it > 0.0 && position >= it * WATCHED_FRACTION } ?: false

/**
 * The saved timestamp doubles as the episode's watched position, which is what the anime
 * screen uses to decide between "start" and "resume". "Seen" is derived rather than stored
 * because the local progress table is the only persistence available.
 */
internal fun LocalChapter.toEpisode(
    book: LocalBook,
    progress: ReadingProgress?,
    index: Int,
): Episode = Episode(
    id = episodeId(),
    animeId = book.animeId(),
    seen = progress.isWatched(),
    bookmark = false,
    fillermark = false,
    // Aniyomi stores both fields in milliseconds despite the "second" in the names.
    lastSecondSeen = (progress?.position ?: 0.0).toLong(),
    totalSeconds = (progress?.duration ?: 0.0).toLong(),
    dateFetch = lastModified,
    sourceOrder = index.toLong(),
    url = location,
    name = title,
    dateUpload = lastModified,
    episodeNumber = EpisodeRecognition.parseEpisodeNumber(book.title, title),
    scanlator = null,
    summary = null,
    previewUrl = null,
    lastModifiedAt = lastModified,
    version = 1L,
    memo = JsonObject.EMPTY,
)

/** A season folder rendered in the series detail; counts come from its episodes and progress. */
internal fun LocalBook.toSeasonItem(
    episodes: List<LocalChapter>,
    progress: List<ReadingProgress>,
): AnimeSeasonItem {
    val byEntry = progress.associateBy { it.entryId }
    val seasonAnime = SeasonAnime(
        anime = toAnime(),
        totalCount = episodes.size.toLong(),
        seenCount = episodes.count { byEntry[it.location].isWatched() }.toLong(),
        bookmarkCount = 0,
        fillermarkCount = 0,
        latestUpload = episodes.maxOfOrNull { it.lastModified } ?: lastModified,
        fetchedAt = 0,
        lastSeen = progress.maxOfOrNull { it.updatedAt } ?: 0L,
    )
    return AnimeSeasonItem(
        seasonAnime = seasonAnime,
        unseenCount = seasonAnime.unseenCount,
        isLocal = true,
    )
}
