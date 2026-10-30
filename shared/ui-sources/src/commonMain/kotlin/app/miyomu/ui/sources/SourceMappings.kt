package app.miyomu.ui.sources

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import eu.kanade.tachiyomi.source.MangaSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.items.chapter.model.Chapter

/** Stable ids must not collide across sources. */
internal fun sourceMangaId(sourceId: String, comicId: String): Long =
    "$sourceId:$comicId".hashCode().toLong() and 0xFFFFFFFFL

internal fun sourceChapterId(chapterId: String): Long =
    chapterId.hashCode().toLong() and 0xFFFFFFFFL

// Ascending chapter numbers keep the list in source order and avoid bogus "missing" rows.
private const val OnlineChapterFlags: Long = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SORT_ASC

/** A display-only source for the ported screens; reads go through [SourceRuntimeManager]. */
internal class OnlineMangaSource(
    override val id: Long,
    override val name: String,
    override val lang: String = "",
) : MangaSource {
    override suspend fun getMangaDetails(manga: SManga): SManga = unsupported()

    override suspend fun getChapterList(manga: SManga): List<SChapter> = unsupported()

    override suspend fun getPageList(chapter: SChapter): List<Page> = unsupported()

    private fun unsupported(): Nothing =
        throw UnsupportedOperationException("online sources are read through SourceRuntimeManager")
}

internal fun ComicSummary.toManga(sourceId: String, now: Long): Manga = Manga.create().copy(
    id = sourceMangaId(sourceId, id),
    source = sourceId.hashCode().toLong(),
    favorite = false,
    lastUpdate = now,
    dateAdded = now,
    url = id,
    title = title,
    author = author,
    status = SManga.UNKNOWN.toLong(),
    chapterFlags = OnlineChapterFlags,
    thumbnailUrl = coverUrl,
    initialized = true,
    lastModifiedAt = now,
    version = 1L,
)

internal fun ComicDetails.toManga(sourceId: String, now: Long): Manga = Manga.create().copy(
    id = sourceMangaId(sourceId, id),
    source = sourceId.hashCode().toLong(),
    favorite = false,
    lastUpdate = now,
    dateAdded = now,
    url = url ?: id,
    title = title,
    author = tags["author"]?.firstOrNull(),
    artist = tags["artist"]?.firstOrNull(),
    description = description,
    chapterFlags = OnlineChapterFlags,
    genre = tags.filterKeys { it != "author" && it != "artist" }
        .values
        .flatten()
        .distinct()
        .takeIf { it.isNotEmpty() },
    status = SManga.UNKNOWN.toLong(),
    thumbnailUrl = coverUrl,
    initialized = true,
    lastModifiedAt = now,
    version = 1L,
)

internal fun app.miyomu.model.Chapter.toDomainChapter(mangaId: Long, index: Int): Chapter = Chapter(
    id = sourceChapterId(id),
    mangaId = mangaId,
    read = false,
    bookmark = false,
    lastPageRead = 0L,
    dateFetch = 0L,
    sourceOrder = index.toLong(),
    url = id,
    name = title,
    dateUpload = 0L,
    chapterNumber = (index + 1).toDouble(),
    scanlator = group,
    lastModifiedAt = 0L,
    version = 1L,
)
