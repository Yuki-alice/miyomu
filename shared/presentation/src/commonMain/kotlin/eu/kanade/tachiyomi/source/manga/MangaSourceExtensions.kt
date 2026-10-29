package eu.kanade.tachiyomi.source.manga

import eu.kanade.tachiyomi.source.MangaSource

/**
 * CMP port: language-filtering display logic from Aniyomi's MangaSourceExtensions is
 * simplified to the source display name; source preferences arrive with source integration.
 */
fun MangaSource.getNameForMangaInfo(): String = name

fun MangaSource.getPreferenceKey(): String = "source_$id"
