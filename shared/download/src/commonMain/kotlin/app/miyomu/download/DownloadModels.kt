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

data class DownloadTask(
    val id: Long,
    val request: DownloadChapterRequest,
    val status: DownloadStatus,
    val pagesDone: Int = 0,
    val pagesTotal: Int = 0,
    val error: String? = null,
)

data class DownloadQueueState(
    val tasks: List<DownloadTask> = emptyList(),
) {
    val activeOrQueued: List<DownloadTask> get() = tasks.filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }
}

/** Answers "is this chapter already on disk" and hands out offline page sources. */
interface DownloadStore {
    fun isDownloaded(itemId: String, chapterKey: String): Boolean

    fun deleteChapter(itemId: String, chapterKey: String)

    fun pageSource(itemId: String, chapterKey: String, chapterTitle: String): app.miyomu.local.ReaderPageSource?
}

/** Sequential comic-page download queue; survives app runs by skipping completed chapters. */
interface DownloadQueue {
    val state: StateFlow<DownloadQueueState>

    fun enqueue(request: DownloadChapterRequest)

    fun cancel(taskId: Long)

    /** Removes terminal tasks from the list (files stay on disk). */
    fun clearFinished()

    fun close()
}
