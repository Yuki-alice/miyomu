package app.miyomu.download

import app.miyomu.source.manager.SourceRuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

/**
 * Sequential comic-page downloader: one worker pulls queued chapters, fetches pages through the
 * source runtime and writes them into the [FileDownloadStore]. Pages already on disk are skipped,
 * so a failed download resumes where it stopped.
 */
class FileDownloadQueue(
    private val runtime: SourceRuntimeManager,
    private val store: FileDownloadStore,
    private val scope: CoroutineScope,
    private val pageDelayMillis: Long = 0L,
    /** Injectable for tests; the app uses the platform-default engine (P1-2 video downloads). */
    private val httpClient: HttpClient? = null,
) : DownloadQueue {

    companion object {
        /** Refuse to start (or keep) downloading when the volume has less free space than this. */
        const val MIN_FREE_BYTES: Long = 64L * 1024 * 1024
    }

    private val mutex = Mutex()
    private val tasks = mutableMapOf<Long, DownloadTask>()
    private val jobs = mutableMapOf<Long, Job>()
    private val ids = AtomicLong(1)

    @Volatile
    private var closed = false

    private val _state = MutableStateFlow(DownloadQueueState())
    override val state: StateFlow<DownloadQueueState> = _state.asStateFlow()

    override fun enqueue(request: DownloadChapterRequest) {
        if (closed) return
        val id = ids.getAndIncrement()
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                tasks[id] = DownloadTask(
                    id = id,
                    request = request,
                    status = if (store.isDownloaded(request.itemId, request.chapterKey)) {
                        DownloadStatus.DONE
                    } else {
                        DownloadStatus.QUEUED
                    },
                )
                publish()
            }
            pump()
        }
    }

    override fun enqueueVideo(request: VideoDownloadRequest) {
        if (closed) return
        val id = ids.getAndIncrement()
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                if (store.isEpisodeDownloaded(request.itemId, request.episodeUrl)) {
                    tasks[id] = DownloadTask(
                        id = id,
                        request = bookkeepingRequest(request),
                        status = DownloadStatus.DONE,
                        video = request,
                    )
                } else {
                    tasks[id] = DownloadTask(
                        id = id,
                        request = bookkeepingRequest(request),
                        status = DownloadStatus.QUEUED,
                        video = request,
                    )
                }
                publish()
            }
            pump()
        }
    }

    /** Video tasks reuse the comic task shape; the episode url rides in comicId/chapterKey. */
    private fun bookkeepingRequest(video: VideoDownloadRequest) = DownloadChapterRequest(
        sourceId = video.sourceId,
        itemId = video.itemId,
        comicId = video.episodeUrl,
        chapterKey = video.episodeUrl,
        chapterTitle = video.episodeTitle,
    )

    /** Cancels the queued/running video task for one episode, if any (P1-2 DELETE action). */
    override fun cancelForEpisode(itemId: String, episodeUrl: String) {
        if (closed) return
        scope.launch(Dispatchers.IO) {
            val task = mutex.withLock {
                tasks.values.firstOrNull { task -> task.video?.let { v -> v.itemId == itemId && v.episodeUrl == episodeUrl } == true }
            }
            if (task != null) cancel(task.id)
        }
    }

    override fun cancel(taskId: Long) {
        if (closed) return
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                jobs.remove(taskId)?.cancel()
                tasks[taskId]?.let { task ->
                    if (task.status == DownloadStatus.QUEUED || task.status == DownloadStatus.RUNNING) {
                        tasks[taskId] = task.copy(status = DownloadStatus.CANCELLED)
                    }
                }
                publish()
            }
            pump()
        }
    }

    override fun clearFinished() {
        if (closed) return
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                tasks.entries.removeIf { entry ->
                    entry.value.status == DownloadStatus.DONE ||
                        entry.value.status == DownloadStatus.FAILED ||
                        entry.value.status == DownloadStatus.CANCELLED
                }
                publish()
            }
        }
    }

    override fun close() {
        closed = true
        jobs.values.forEach { it.cancel() }
    }

    /** Starts the next queued chapter while none is running. Safe to call from anywhere. */
    private suspend fun pump() {
        val next = mutex.withLock {
            if (closed) return
            val running = tasks.values.any { it.status == DownloadStatus.RUNNING }
            if (running) return
            val candidate = tasks.values
                .filter { it.status == DownloadStatus.QUEUED }
                .minByOrNull { it.id }
                ?: return
            tasks[candidate.id] = candidate.copy(status = DownloadStatus.RUNNING)
            publish()
            candidate
        }
        val job = scope.launch(Dispatchers.IO) { runTask(next) }
        mutex.withLock {
            jobs[next.id] = job
            publish()
        }
    }

    private suspend fun runTask(task: DownloadTask) {
        try {
            val video = task.video
            if (video != null) {
                runVideoTask(task, video)
                return
            }
            val request = task.request
            val pages = runtime.pages(request.sourceId, request.comicId, request.chapterKey)
            if (pages.size > FileDownloadStore.MAX_PAGES) {
                throw IllegalArgumentException("chapter has ${pages.size} pages, over the ${FileDownloadStore.MAX_PAGES} limit")
            }
            setStatus(task.id) { it.copy(pagesTotal = pages.size) }
            for ((index, page) in pages.withIndex()) {
                val existing = store.pageFile(request.itemId, request.chapterKey, index)
                if (existing.isFile && existing.length() > 0) continue
                // P0-7: surface an actionable message instead of a raw ENOSPC mid-write.
                if (store.usableBytes() < MIN_FREE_BYTES) {
                    throw IllegalStateException(
                        "存储空间不足：剩余不足 ${MIN_FREE_BYTES / (1024 * 1024)} MiB，请清理后重试",
                    )
                }
                val bytes = runtime.loadImage(request.sourceId, page.url, page.headers)
                store.writePage(request.itemId, request.chapterKey, index, bytes)
                setStatus(task.id) { it.copy(pagesDone = index + 1) }
                if (pageDelayMillis > 0) kotlinx.coroutines.delay(pageDelayMillis)
            }
            store.markComplete(request.itemId, request.chapterKey, pages.size)
            setStatus(task.id) { it.copy(status = DownloadStatus.DONE, pagesDone = pages.size) }
        } catch (error: kotlinx.coroutines.CancellationException) {
            mutex.withLock {
                tasks[task.id]?.let { if (it.status == DownloadStatus.RUNNING) tasks[task.id] = it.copy(status = DownloadStatus.CANCELLED) }
                publish()
            }
            throw error
        } catch (error: Exception) {
            setStatus(task.id) { it.copy(status = DownloadStatus.FAILED, error = error.message ?: error::class.simpleName) }
        } finally {
            mutex.withLock { jobs.remove(task.id) }
            pump()
        }
    }

    /**
     * P1-2 视频按集: resolves the episode through the source runtime and streams the MP4
     * direct stream to disk with byte progress. HLS/DASH fail with an actionable message —
     * segmented download + muxing is a dedicated slice.
     */
    private suspend fun runVideoTask(task: DownloadTask, video: VideoDownloadRequest) {
        val client = httpClient ?: HttpClient()
        try {
            val resolved = runtime.resolveVideo(video.sourceId, video.episodeUrl)
            val stream = resolved.bestStream()
            if (stream == null) {
                throw IllegalStateException("该剧集没有可用的视频流")
            }
            if (stream.type != "mp4" && stream.type != "hls") {
                throw IllegalStateException("该流的格式（${stream.type}）暂不支持离线下载，仅支持 MP4/HLS")
            }
            val ext = if (stream.type == "hls") "ts" else "mp4"
            val response = client.get(stream.url) {
                resolved.headers.forEach { (name, value) -> headers.append(name, value) }
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("视频下载失败：HTTP ${response.status.value}")
            }
            var done = 0L
            val target = store.episodeFile(video.itemId, video.episodeUrl, ext)
            if (stream.type == "hls") {
                // P1-2: HLS media segments are concatenated into one MPEG-TS file (players
                // handle TS natively; remux-to-MP4 is a dedicated slice).
                done = downloadHls(client, stream.url, target) { bytes ->
                    setStatus(task.id) { it.copy(bytesDone = bytes) }
                }
            } else {
                val response = client.get(stream.url) {
                    resolved.headers.forEach { (name, value) -> headers.append(name, value) }
                }
                if (!response.status.isSuccess()) {
                    throw IllegalStateException("视频下载失败：HTTP ${response.status.value}")
                }
                val total = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                setStatus(task.id) { it.copy(bytesTotal = total) }
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(64 * 1024)
                target.parentFile?.mkdirs()
                java.io.FileOutputStream(target).use { out ->
                    while (!channel.isClosedForRead) {
                        if (store.usableBytes() < MIN_FREE_BYTES) {
                            throw IllegalStateException(
                                "存储空间不足：剩余不足 ${MIN_FREE_BYTES / (1024 * 1024)} MiB，请清理后重试",
                            )
                        }
                        val read = channel.readAvailable(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        done += read
                        setStatus(task.id) { it.copy(bytesDone = done) }
                    }
                }
            }
            store.markEpisodeComplete(video.itemId, video.episodeUrl)
            setStatus(task.id) { it.copy(status = DownloadStatus.DONE, bytesDone = done) }
        } finally {
            if (httpClient == null) client.close()
        }
    }

    private suspend fun setStatus(taskId: Long, transform: (DownloadTask) -> DownloadTask) {
        mutex.withLock {
            tasks[taskId]?.let { current ->
                // A cancel racing the start must win over late DONE/FAILED writes.
                if (current.status != DownloadStatus.CANCELLED) {
                    tasks[taskId] = transform(current)
                }
                publish()
            }
        }
    }

    private suspend fun publish() {
        _state.update { current ->
            current.copy(tasks = tasks.values.sortedBy { it.id })
        }
    }
}
