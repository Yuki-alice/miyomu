package eu.kanade.tachiyomi.animesource

/**
 * CMP port of Aniyomi's AnimeSource, reduced to what the ported anime screens need.
 * Episode/hoster loading arrives with online anime sources in Phase C.
 */
interface AnimeSource {

    val id: Long

    val name: String

    val lang: String
}

interface ConfigurableAnimeSource : AnimeSource