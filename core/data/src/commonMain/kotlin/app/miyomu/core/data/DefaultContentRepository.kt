package app.miyomu.core.data

import app.miyomu.core.model.AppError
import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.ChapterSummary
import app.miyomu.core.model.ContentDetails
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentRating
import app.miyomu.core.model.ContentState
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.model.ContentType
import app.miyomu.core.model.Outcome
import app.miyomu.core.model.PageSummary
import app.miyomu.core.model.SourceId
import app.miyomu.core.model.SourceOption
import app.miyomu.core.source.SourceRegistry
import kotlinx.coroutines.CancellationException
import org.skepsun.kototoro.core.source.SourceChapter
import org.skepsun.kototoro.core.source.SourceContent
import org.skepsun.kototoro.core.source.SourceDetailsFetchMode

class DefaultContentRepository(
    private val registry: SourceRegistry,
) : ContentRepository {
    private val cache = mutableMapOf<Long, SourceContent>()
    private val chapterCache = mutableMapOf<Long, SourceChapter>()
    private var selectedSource: String? = null

    override suspend fun sources(): List<SourceOption> = registry.sources().map { ref ->
        SourceOption(
            id = SourceId(ref.name),
            name = ref.name,
            contentType = when (ref.contentType) {
                "NOVEL" -> ContentType.NOVEL
                "VIDEO" -> ContentType.VIDEO
                else -> ContentType.MANGA
            },
        )
    }

    override suspend fun selectSource(sourceName: String) {
        registry.runtime(sourceName)
        selectedSource = sourceName
        cache.clear()
        chapterCache.clear()
    }

    private suspend fun runtime() = if (selectedSource == null) registry.primary() else registry.runtime(selectedSource!!)

    override suspend fun browse(offset: Int, limit: Int, query: String?): Outcome<List<ContentSummary>> {
        return try {
            val runtime = runtime()
            val source = runtime.getSources().first()
            val page = runtime.getList(
                sourceName = source.name,
                offset = offset,
                order = null,
                filter = query?.takeIf { it.isNotBlank() }?.let {
                    org.skepsun.kototoro.core.source.SourceFilter(query = it)
                },
            )
            page.forEach { cache[it.id] = it }
            Outcome.Success(page.map { it.toSummary() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Outcome.Failure(AppError.Network(e))
        }
    }

    override suspend fun details(contentId: ContentId): Outcome<ContentDetails> {
        return try {
            val runtime = runtime()
            val source = runtime.getSources().first()
            val content = cache[contentId.value] ?: loadById(runtime, source.name, contentId.value)
                ?: return Outcome.Failure(AppError.NotFound("content ${contentId.value}"))
            val detailed = runtime.getDetails(content, SourceDetailsFetchMode.ALLOW_CACHE)
            cache[detailed.id] = detailed
            Outcome.Success(
                ContentDetails(
                    summary = detailed.toSummary(),
                    description = detailed.description,
                    chapters = detailed.chapters.orEmpty().map { chapter ->
                        ChapterSummary(
                            id = ChapterId(chapter.id),
                            title = chapter.title ?: "Chapter ${chapter.number}",
                            number = chapter.number,
                            uploadedAt = chapter.uploadDate,
                        )
                    },
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Outcome.Failure(AppError.Network(e))
        }
    }

    override suspend fun pages(contentId: ContentId, chapterId: ChapterId): Outcome<List<PageSummary>> {
        return try {
            val runtime = runtime()
            val source = runtime.getSources().first()
            var chapter = chapterCache[chapterId.value]
            if (chapter == null) {
                val content = cache[contentId.value] ?: loadById(runtime, source.name, contentId.value)
                    ?: return Outcome.Failure(AppError.NotFound("content ${contentId.value}"))
                val detailed = runtime.getDetails(content, SourceDetailsFetchMode.ALLOW_CACHE)
                detailed.chapters.orEmpty().forEach { chapterCache[it.id] = it }
                chapter = chapterCache[chapterId.value]
            }
            if (chapter == null) return Outcome.Failure(AppError.NotFound("chapter ${chapterId.value}"))
            val pages = runtime.getPages(chapter, null)
            Outcome.Success(pages.mapIndexed { index, page ->
                PageSummary(
                    id = page.id,
                    index = index,
                    url = page.url,
                    headers = page.headers.orEmpty(),
                )
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Outcome.Failure(AppError.Network(e))
        }
    }

    private suspend fun loadById(
        runtime: org.skepsun.kototoro.core.source.SourceRuntime,
        sourceName: String,
        id: Long,
    ): SourceContent? {
        var offset = 0
        while (true) {
            val page = runtime.getList(sourceName, offset, null, null)
            page.forEach { cache[it.id] = it }
            page.firstOrNull { it.id == id }?.let { return it }
            if (page.isEmpty()) return null
            offset += page.size
            if (offset > 500) return null
        }
    }
}

private fun SourceContent.toSummary(): ContentSummary = ContentSummary(
    id = ContentId(id),
    source = SourceId(source.name),
    title = title,
    coverUrl = coverUrl,
    authors = authors.toList(),
    contentType = when (source.contentType) {
        "NOVEL" -> ContentType.NOVEL
        "VIDEO" -> ContentType.VIDEO
        else -> ContentType.MANGA
    },
    rating = rating.takeIf { it > 0f },
    state = ContentState.entries.firstOrNull { it.name == state } ?: ContentState.UNKNOWN,
)
