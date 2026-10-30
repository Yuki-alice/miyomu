package app.miyomu.ui.library

import app.miyomu.local.LocalBook
import app.miyomu.model.ReadingProgress
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryItem
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.library.manga.LibraryManga

/**
 * Miyomu's scanned [LocalBook] has no Aniyomi row behind it, so the ported library screens get a
 * synthetic [Manga]. Local books all live under the reserved source id 0, which is what
 * `Manga.isLocal()` (and therefore the local badge / cover handling) keys off.
 */
const val LOCAL_SOURCE_ID = 0L

/**
 * Stable synthetic id for a book. The library id is a path string; hashing keeps the id stable
 * across scans and restarts, which matters because the reader and the progress table key on it.
 */
fun LocalBook.mangaId(): Long = id.hashCode().toLong() and 0xFFFFFFFFL

fun LocalBook.toManga(): Manga = Manga(
    id = mangaId(),
    source = LOCAL_SOURCE_ID,
    favorite = true,
    lastUpdate = lastModified,
    nextUpdate = 0L,
    fetchInterval = 0,
    dateAdded = lastModified,
    viewerFlags = 0L,
    chapterFlags = 0L,
    coverLastModified = lastModified,
    url = location,
    title = title,
    artist = null,
    author = null,
    description = null,
    genre = null,
    status = SManga.UNKNOWN.toLong(),
    thumbnailUrl = null,
    updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
    initialized = true,
    lastModifiedAt = lastModified,
    favoriteModifiedAt = null,
    version = 1L,
)

/**
 * A local book is a single readable unit (one directory/archive), so it maps to exactly one
 * chapter: read once, unread until a progress row exists.
 */
fun LocalBook.toLibraryManga(progress: ReadingProgress?): LibraryManga = LibraryManga(
    manga = toManga(),
    category = 0L,
    totalChapters = 1L,
    readCount = if (progress != null) 1L else 0L,
    bookmarkCount = 0L,
    latestUpload = lastModified,
    chapterFetchedAt = lastModified,
    lastRead = progress?.updatedAt ?: 0L,
)

fun LocalBook.toLibraryItem(progress: ReadingProgress?): MangaLibraryItem = MangaLibraryItem(
    libraryManga = toLibraryManga(progress),
    downloadCount = 0L,
    unreadCount = if (progress != null) 0L else 1L,
    isLocal = true,
    sourceLanguage = "",
)
