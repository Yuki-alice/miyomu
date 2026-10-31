package app.miyomu.core.data

import app.miyomu.core.model.ChapterId
import app.miyomu.core.model.ContentId
import app.miyomu.core.model.ContentSummary
import app.miyomu.core.database.FavouriteEntity
import app.miyomu.core.database.MiyomuDatabase
import app.miyomu.core.database.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ReadingProgress(
    val contentId: ContentId,
    val chapterId: ChapterId,
    val chapterTitle: String?,
    val page: Int,
    val scroll: Float,
    val percent: Float,
    val updatedAt: Long,
)

interface LibraryRepository {
    fun observeFavourites(): Flow<List<ContentSummary>>
    fun observeProgress(contentId: ContentId): Flow<ReadingProgress?>
    suspend fun setFavourite(summary: ContentSummary, favourite: Boolean)
    suspend fun saveProgress(
        contentId: ContentId,
        chapterId: ChapterId,
        chapterTitle: String?,
        page: Int,
        scroll: Float,
        percent: Float,
    )
}

@OptIn(ExperimentalTime::class)
class DefaultLibraryRepository(
    private val database: MiyomuDatabase,
) : LibraryRepository {
    private val favourites = database.favouriteDao()
    private val progress = database.readingProgressDao()

    override fun observeFavourites(): Flow<List<ContentSummary>> =
        favourites.observeAll().map { rows ->
            rows.map { row ->
                ContentSummary(
                    id = ContentId(row.contentId),
                    source = app.miyomu.core.model.SourceId(row.sourceName),
                    title = row.title,
                    coverUrl = row.coverUrl,
                    authors = row.authors.split('\u001f').filter { it.isNotBlank() },
                    contentType = app.miyomu.core.model.ContentType.entries
                        .firstOrNull { it.name == row.contentType }
                        ?: app.miyomu.core.model.ContentType.UNKNOWN,
                )
            }
        }

    override fun observeProgress(contentId: ContentId): Flow<ReadingProgress?> =
        progress.observe(contentId.value).map { row ->
            row?.let {
                ReadingProgress(
                    contentId = ContentId(it.contentId),
                    chapterId = ChapterId(it.chapterId),
                    chapterTitle = it.chapterTitle,
                    page = it.page,
                    scroll = it.scroll,
                    percent = it.percent,
                    updatedAt = it.updatedAt,
                )
            }
        }

    override suspend fun setFavourite(summary: ContentSummary, favourite: Boolean) {
        if (!favourite) {
            favourites.delete(summary.id.value)
            return
        }
        val now = Clock.System.now().toEpochMilliseconds()
        favourites.upsert(
            FavouriteEntity(
                contentId = summary.id.value,
                sourceName = summary.source.value,
                title = summary.title,
                coverUrl = summary.coverUrl,
                authors = summary.authors.joinToString("\u001f"),
                contentType = summary.contentType.name,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun saveProgress(
        contentId: ContentId,
        chapterId: ChapterId,
        chapterTitle: String?,
        page: Int,
        scroll: Float,
        percent: Float,
    ) {
        progress.upsert(
            ReadingProgressEntity(
                contentId = contentId.value,
                chapterId = chapterId.value,
                chapterTitle = chapterTitle,
                page = page,
                scroll = scroll,
                percent = percent,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }
}
