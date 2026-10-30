package app.miyomu.download

import kotlinx.coroutines.flow.StateFlow

/** A chapter queued for offline download (comic pages only; video downloads are out of scope). */
data class DownloadChapterRequest(
    val sourceId: String,
    /** Library item id this chapter belongs to, e.g. `sourceId:comicId`. */
    val itemId: String,
    val comicId: String,
    val chapterKey: String,
    val chapterTitle: String,
)

enum class DownloadStatus { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

/** One video episode queued for offline download (P1-2 视频按集；MP4 直链). */
data class VideoDownloadRequest(
    val sourceId: String,
    val itemId: String,
    val episodeUrl: String,
    val episodeTitle: String,
)

data class DownloadTask(
    val id: Long,
    val request: DownloadChapterRequest,
    val status: DownloadStatus,
    val pagesDone: Int = 0,
    val pagesTotal: Int = 0,
    val error: String? = null,
    /** Non-null for video episode tasks (P1-2); [request] is synthesized for bookkeeping. */
    val video: VideoDownloadRequest? = null,
    val bytesDone: Long = 0,
    val bytesTotal: Long = 0,
)

data class DownloadQueueState(
    val tasks: List<DownloadTask> = emptyList(),
) {
    val activeOrQueued: List<DownloadTask> get() = tasks.filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }
}

/** A novel book with cached chapters, surfaced in the unified download center (P1-2). */
data class NovelCachedBook(
    val itemId: String,
    /** Display title recorded at prefetch time; null for entries cached before naming. */
    val title: String?,
    val chapterCount: Int,
)

/** Answers "is this chapter already on disk" and hands out offline page sources. */
interface DownloadStore {
    fun isDownloaded(itemId: String, chapterKey: String): Boolean

    fun deleteChapter(itemId: String, chapterKey: String)

    /** Removes every downloaded chapter (data settings). */
    fun clearAll()

    fun pageSource(itemId: String, chapterKey: String, chapterTitle: String): app.miyomu.local.ReaderPageSource?
}

/** Sequential comic-page download queue; survives app runs by skipping completed chapters. */
interface DownloadQueue {
    val state: StateFlow<DownloadQueueState>

    fun enqueue(request: DownloadChapterRequest)

    /** Queues one video episode for offline download (MP4 direct streams; P1-2). */
    fun enqueueVideo(request: VideoDownloadRequest)

    /** Cancels the queued/running video task for one episode, if any (P1-2 DELETE action). */
    fun cancelForEpisode(itemId: String, episodeUrl: String)

    fun cancel(taskId: Long)

    /** Removes terminal tasks from the list (files stay on disk). */
    fun clearFinished()

    fun close()
}
