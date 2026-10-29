package app.miyomu.ui

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.ResolvedVideo
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.manager.SourceRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeSourceRuntimeManager(
    state: SourceRuntimeState = SourceRuntimeState(),
    private val searchResults: List<ComicSummary> = emptyList(),
    private val details: ComicDetails? = null,
    private val pages: List<PageRef> = emptyList(),
    private val imageBytes: ByteArray = ByteArray(0),
) : SourceRuntimeManager {
    private val _state = MutableStateFlow(state)
    override val state: StateFlow<SourceRuntimeState> = _state.asStateFlow()

    override suspend fun refresh() = Unit

    override suspend fun search(sourceId: String, query: String, page: Int): Paged<ComicSummary> =
        Paged(searchResults, hasNext = false)

    override suspend fun details(sourceId: String, comicId: String): ComicDetails =
        details ?: ComicDetails(id = comicId, title = comicId)

    override suspend fun pages(sourceId: String, comicId: String, chapterId: String): List<PageRef> =
        pages

    override suspend fun resolveVideo(sourceId: String, episodeUrl: String): ResolvedVideo =
        ResolvedVideo(streams = emptyList(), headers = emptyMap())

    override suspend fun loadImage(sourceId: String, url: String, headers: Map<String, String>): ByteArray =
        imageBytes

    override fun close() = Unit
}
