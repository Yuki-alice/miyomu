package app.miyomu.source

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.SourceDescriptor

interface ComicSource {
    val descriptor: SourceDescriptor

    suspend fun search(
        query: String,
        page: Int = 1,
        options: List<String> = emptyList(),
    ): Paged<ComicSummary>

    suspend fun details(comicId: String): ComicDetails

    suspend fun pages(comicId: String, chapterId: String?): List<PageRef>
}

interface ManagedComicSource : ComicSource {
    suspend fun loadImage(url: String, headers: Map<String, String> = emptyMap()): ByteArray

    fun close()
}
