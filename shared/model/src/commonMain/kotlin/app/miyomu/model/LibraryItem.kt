package app.miyomu.model

import kotlinx.serialization.Serializable

@Serializable
enum class LibraryStatus {
    UNKNOWN,
    ONGOING,
    COMPLETED,
    HIATUS,
    CANCELLED,
}

@Serializable
data class LibraryItem(
    val id: String,
    val mediaType: MediaType,
    val sourceId: String,
    val sourceKey: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String? = null,
    val description: String? = null,
    /** Free-form labels edited in bulk (P1-3); serialized as a JSON array in backups. */
    val tags: List<String> = emptyList(),
    val status: LibraryStatus = LibraryStatus.UNKNOWN,
    val addedAt: Long,
    val updatedAt: Long,
    val lastViewedAt: Long? = null,
) {
    companion object {
        fun idOf(sourceId: String, sourceKey: String): String = "$sourceId:$sourceKey"

        fun from(
            source: SourceDescriptor,
            comic: ComicSummary,
            now: Long,
        ): LibraryItem = LibraryItem(
            id = idOf(source.id, comic.id),
            mediaType = source.mediaType,
            sourceId = source.id,
            sourceKey = comic.id,
            title = comic.title,
            coverUrl = comic.coverUrl,
            author = comic.author,
            addedAt = now,
            updatedAt = now,
        )
    }
}
