package app.miyomu.source.manager

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import kotlinx.coroutines.flow.StateFlow

/** A loaded source, as shown to the UI. */
data class SourceRuntimeInfo(
    val id: String,
    val name: String,
    val lang: String,
    val version: String,
    val mediaType: String,
)

data class SourceRuntimeState(
    val sources: List<SourceRuntimeInfo> = emptyList(),
    val loading: Boolean = false,
    /** Source id -> load failure message. A broken source does not hide the others. */
    val errors: Map<String, String> = emptyMap(),
)

/**
 * Loads installed JS packages into the runtime and exposes their read path. Implementations
 * own one QuickJS runtime per source and must be closed with the app.
 */
interface SourceRuntimeManager {
    val state: StateFlow<SourceRuntimeState>

    suspend fun refresh()

    suspend fun search(sourceId: String, query: String, page: Int = 1): Paged<ComicSummary>

    suspend fun details(sourceId: String, comicId: String): ComicDetails

    suspend fun pages(sourceId: String, comicId: String, chapterId: String): List<PageRef>

    suspend fun loadImage(sourceId: String, url: String, headers: Map<String, String>): ByteArray

    fun close()
}
