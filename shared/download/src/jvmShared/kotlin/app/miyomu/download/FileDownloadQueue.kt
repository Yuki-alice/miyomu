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
) : DownloadQueue {

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
            val request = task.request
            val pages = runtime.pages(request.sourceId, request.comicId, request.chapterKey)
            if (pages.size > FileDownloadStore.MAX_PAGES) {
                throw IllegalArgumentException("chapter has ${pages.size} pages, over the ${FileDownloadStore.MAX_PAGES} limit")
            }
            setStatus(task.id) { it.copy(pagesTotal = pages.size) }
            for ((index, page) in pages.withIndex()) {
                val existing = store.pageFile(request.itemId, request.chapterKey, index)
                if (existing.isFile && existing.length() > 0) continue
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
