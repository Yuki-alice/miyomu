package app.miyomu.ui.entries

import app.miyomu.local.LocalBook
import app.miyomu.model.ReadingProgress
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

/** Stable anime id derived from the on-disk location, matching the manga/library mapping. */
internal fun LocalBook.animeId(): Long = id.hashCode().toLong() and 0xFFFFFFFFL

/** Stable episode id derived from the on-disk location. */
internal fun LocalBook.episodeId(): Long = location.hashCode().toLong() and 0xFFFFFFFFL

/**
 * A local video file is a single-episode anime. Season support (folder = series) needs a
 * scanner change and is deliberately left out of this slice.
 */
internal fun LocalBook.toAnime(): Anime = Anime.create().copy(
    id = animeId(),
    source = 0L,
    favorite = true,
    lastUpdate = lastModified,
    dateAdded = lastModified,
    url = location,
    title = title,
    status = SAnime.UNKNOWN.toLong(),
    initialized = true,
    lastModifiedAt = lastModified,
    version = 1L,
    fetchType = FetchType.Episodes,
)

/** Matches Aniyomi's default `progressPreference` for marking an episode watched. */
private const val WATCHED_FRACTION = 0.85

/**
 * The saved timestamp doubles as the episode's watched position, which is what the anime
 * screen uses to decide between "start" and "resume". "Seen" is derived rather than stored
 * because the local progress table is the only persistence available.
 */
internal fun LocalBook.toEpisode(progress: ReadingProgress?): Episode = Episode(
    id = episodeId(),
    animeId = animeId(),
    seen = progress?.duration?.let { it > 0.0 && progress.position >= it * WATCHED_FRACTION } ?: false,
    bookmark = false,
    fillermark = false,
    // Aniyomi stores both fields in milliseconds despite the "second" in the names.
    lastSecondSeen = (progress?.position ?: 0.0).toLong(),
    totalSeconds = (progress?.duration ?: 0.0).toLong(),
    dateFetch = lastModified,
    sourceOrder = 0L,
    url = location,
    name = title,
    dateUpload = lastModified,
    episodeNumber = 1.0,
    scanlator = null,
    summary = null,
    previewUrl = null,
    lastModifiedAt = lastModified,
    version = 1L,
    memo = JsonObject.EMPTY,
)
