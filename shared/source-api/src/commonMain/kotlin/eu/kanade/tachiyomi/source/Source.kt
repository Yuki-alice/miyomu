package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

/**
 * CMP port of the source-api MangaSource: reduced to the members the ported screens and
 * stub sources need; network/observable plumbing arrives with online source integration.
 */
interface MangaSource {

    val id: Long

    val name: String

    val lang: String

    suspend fun getMangaDetails(manga: SManga): SManga

    suspend fun getChapterList(manga: SManga): List<SChapter>

    suspend fun getPageList(chapter: SChapter): List<Page>
}

interface ConfigurableSource : MangaSource
