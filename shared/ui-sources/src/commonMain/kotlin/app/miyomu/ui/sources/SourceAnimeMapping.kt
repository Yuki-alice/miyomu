package app.miyomu.ui.sources

import app.miyomu.contract.EpisodeRecord
import app.miyomu.model.AnimeEpisode
import app.miyomu.model.ComicDetails
import app.miyomu.model.ReadingProgress
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import tachiyomi.domain.entries.anime.model.Anime
import tachiyomi.domain.items.episode.model.Episode

/** Stable anime id derived from sourceId + item id; matches the manga/library mapping scheme. */
internal fun sourceAnimeId(sourceId: String, animeId: String): Long =
    "$sourceId:$animeId".hashCode().toLong() and 0xFFFFFFFFL

internal fun sourceEpisodeId(episodeKey: String): Long =
    episodeKey.hashCode().toLong() and 0xFFFFFFFFL

internal fun ComicDetails.toAnime(sourceId: String, favorite: Boolean, now: Long): Anime =
    Anime.create().copy(
        id = sourceAnimeId(sourceId, id),
        source = sourceId.hashCode().toLong(),
        favorite = favorite,
        lastUpdate = now,
        dateAdded = now,
        url = url ?: id,
        title = title,
        author = tags["author"]?.firstOrNull(),
        description = description,
        genre = tags.filterKeys { it != "author" && it != "artist" }
            .values
            .flatten()
            .distinct()
            .takeIf { it.isNotEmpty() },
        status = SAnime.UNKNOWN.toLong(),
        thumbnailUrl = coverUrl,
        initialized = true,
        lastModifiedAt = now,
        version = 1L,
        fetchType = FetchType.Episodes,
    )

/**
 * Seen/bookmark state comes from the D1 episode table; the saved timestamp doubles as the
 * resume position, exactly like the local video mapping.
 */
internal fun AnimeEpisode.toDomainEpisode(
    animeId: Long,
    index: Int,
    record: EpisodeRecord?,
    progress: ReadingProgress?,
): Episode = Episode(
    id = sourceEpisodeId(key),
    animeId = animeId,
    seen = record?.seen ?: false,
    bookmark = record?.bookmark ?: false,
    fillermark = false,
    // Aniyomi stores both fields in milliseconds despite the "second" in the names.
    lastSecondSeen = (progress?.position ?: 0.0).toLong(),
    totalSeconds = (progress?.duration ?: 0.0).toLong(),
    dateFetch = record?.fetchedAt ?: 0L,
    sourceOrder = index.toLong(),
    url = url,
    name = name,
    dateUpload = record?.fetchedAt ?: 0L,
    episodeNumber = number ?: (index + 1).toDouble(),
    scanlator = null,
    summary = null,
    previewUrl = null,
    lastModifiedAt = record?.fetchedAt ?: 0L,
    version = 1L,
    memo = JsonObject.EMPTY,
)
