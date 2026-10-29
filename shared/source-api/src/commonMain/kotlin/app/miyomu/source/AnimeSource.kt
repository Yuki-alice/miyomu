package app.miyomu.source

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.Paged
import app.miyomu.model.ResolvedVideo
import app.miyomu.model.SourceDescriptor

/**
 * Read path for a JS anime source (ADR-0014). Search/detail reuse the comic item shapes so one
 * browse/detail UI can render both; anime specifics live in [ComicDetails.episodes] and
 * [resolve], which turns an episode url into stream candidates plus anti-leech headers.
 */
interface AnimeSource {
    val descriptor: SourceDescriptor

    suspend fun search(query: String, page: Int = 1): Paged<ComicSummary>

    suspend fun details(animeId: String): ComicDetails

    suspend fun resolve(episodeUrl: String): ResolvedVideo
}

interface ManagedAnimeSource : AnimeSource {
    suspend fun loadImage(url: String, headers: Map<String, String> = emptyMap()): ByteArray

    fun close()
}
