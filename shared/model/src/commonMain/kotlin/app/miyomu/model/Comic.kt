package app.miyomu.model

import kotlinx.serialization.Serializable

@Serializable
data class ComicSummary(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class ComicDetails(
    val id: String,
    val title: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),
    val chapters: List<Chapter> = emptyList(),
    /** Anime episodes (ADR-0014); comic sources leave it empty. */
    val episodes: List<AnimeEpisode> = emptyList(),
    val updateTime: String? = null,
    val url: String? = null,
)

/**
 * One playable episode of an anime. `key` is the stable identity stored in the data layer,
 * `url` is what the source `resolve`s into stream candidates, `line` is the 0-based play route
 * (线路) for sites with mirrors.
 */
@Serializable
data class AnimeEpisode(
    val key: String,
    val name: String,
    val url: String,
    val line: Int = 0,
    val number: Double? = null,
)

@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val group: String? = null,
)

@Serializable
data class PageRef(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
