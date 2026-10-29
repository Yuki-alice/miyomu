package eu.kanade.tachiyomi.source.anime

import eu.kanade.tachiyomi.animesource.AnimeSource

/**
 * CMP port: language-filtering display logic from Aniyomi's AnimeSourceExtensions is
 * simplified to the source display name; source preferences arrive with source integration.
 */
fun AnimeSource.getNameForAnimeInfo(): String = name

fun AnimeSource.getPreferenceKey(): String = "source_$id"