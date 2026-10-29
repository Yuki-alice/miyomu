package tachiyomi.domain.history.manga.model

import tachiyomi.domain.entries.manga.model.MangaCover

/**
 * One reading-history row joined with its manga metadata, shaped like Aniyomi's history query
 * result; Miyomu composes it from the progress table (ADR-0011 data substitution).
 */
data class MangaHistoryWithRelations(
    val id: Long,
    val chapterId: Long,
    val mangaId: Long,
    val title: String,
    val chapterNumber: Double,
    val readAt: Long?,
    val readDuration: Long,
    val coverData: MangaCover,
)
