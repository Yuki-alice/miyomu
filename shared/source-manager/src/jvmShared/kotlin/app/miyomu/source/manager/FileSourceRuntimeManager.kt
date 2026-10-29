package app.miyomu.source.manager

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.source.ManagedComicSource
import app.miyomu.source.repo.InstalledSource
import app.miyomu.source.repo.SourceInstallManager
import app.miyomu.source.runtime.loadJsComicSource
import kotlinx.coroutines.CancellationException
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
import kotlinx.io.files.Path
import java.io.File

/**
 * Loads every installed package with [loadJsComicSource] and keeps the runtimes in memory.
 * The script key must match the package id, so a package cannot impersonate another source.
 */
class FileSourceRuntimeManager(
    private val installs: SourceInstallManager,
    private val scope: CoroutineScope,
) : SourceRuntimeManager {

    private data class LoadedSource(
        val version: String,
        val info: SourceRuntimeInfo,
        val source: ManagedComicSource,
    )

    private val mutex = Mutex()
    private val loaded = mutableMapOf<String, LoadedSource>()
    private val _state = MutableStateFlow(SourceRuntimeState(loading = true))
    override val state: StateFlow<SourceRuntimeState> = _state.asStateFlow()

    private var closed = false
    private val collectionJob: Job = scope.launch(Dispatchers.IO) {
        installs.ensureLoaded()
        installs.installed.collect { sync(it) }
    }

    override suspend fun refresh() {
        installs.ensureLoaded()
        sync(installs.installed.value)
    }

    private suspend fun sync(records: List<InstalledSource>) = mutex.withLock {
        if (closed) return@withLock
        _state.update { it.copy(loading = true) }

        val desired = records.associateBy { it.id }
        (loaded.keys - desired.keys).forEach { id -> loaded.remove(id)?.source?.close() }

        val errors = mutableMapOf<String, String>()
        val infos = mutableListOf<SourceRuntimeInfo>()
        for (record in records) {
            val cached = loaded[record.id]
            if (cached != null && cached.version == record.version) {
                infos += cached.info
                continue
            }
            cached?.source?.close()
            loaded.remove(record.id)

            runCatching {
                val scriptFile = File(record.scriptPath)
                val source = loadJsComicSource(
                    script = scriptFile.readText(),
                    dataDir = Path(scriptFile.absoluteFile.parentFile.absolutePath),
                    expectedKey = record.id,
                )
                LoadedSource(
                    version = record.version,
                    info = SourceRuntimeInfo(
                        id = record.id,
                        name = source.descriptor.name,
                        lang = record.lang,
                        version = record.version,
                        mediaType = source.descriptor.mediaType.name,
                    ),
                    source = source,
                ).also { loaded[record.id] = it }
            }.onSuccess { infos += it.info }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    errors[record.id] = error.message ?: error::class.simpleName ?: "无法加载源"
                }
        }

        _state.value = SourceRuntimeState(
            sources = infos.sortedBy { it.name.lowercase() },
            loading = false,
            errors = errors,
        )
    }

    override suspend fun search(sourceId: String, query: String, page: Int): Paged<ComicSummary> =
        source(sourceId).search(query, page)

    override suspend fun details(sourceId: String, comicId: String): ComicDetails =
        source(sourceId).details(comicId)

    override suspend fun pages(sourceId: String, comicId: String, chapterId: String): List<PageRef> =
        source(sourceId).pages(comicId, chapterId)

    override suspend fun loadImage(
        sourceId: String,
        url: String,
        headers: Map<String, String>,
    ): ByteArray = source(sourceId).loadImage(url, headers)

    override fun close() {
        closed = true
        collectionJob.cancel()
        loaded.values.forEach { it.source.close() }
        loaded.clear()
        _state.value = SourceRuntimeState()
    }

    private fun source(sourceId: String): ManagedComicSource =
        loaded[sourceId]?.source ?: error("源未加载：$sourceId")
}
