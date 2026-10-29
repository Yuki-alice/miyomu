package app.miyomu.source.manager

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.ResolvedVideo
import app.miyomu.source.ManagedAnimeSource
import app.miyomu.source.ManagedComicSource
import app.miyomu.source.repo.InstalledSource
import app.miyomu.source.repo.SourceInstallManager
import app.miyomu.source.runtime.isAnimeSourceScript
import app.miyomu.source.runtime.loadJsAnimeSource
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
 * Loads every installed package into a QuickJS runtime and keeps the runtimes in memory.
 * A script declaring `class … extends AnimeSource` loads as an anime source; anything else as
 * a comic source. The script key must match the package id, so a package cannot impersonate
 * another source.
 */
class FileSourceRuntimeManager(
    private val installs: SourceInstallManager,
    private val scope: CoroutineScope,
) : SourceRuntimeManager {

    private class LoadedSource(
        val version: String,
        val info: SourceRuntimeInfo,
        val comic: ManagedComicSource?,
        val anime: ManagedAnimeSource?,
    ) {
        fun close() {
            comic?.close()
            anime?.close()
        }
    }

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
        (loaded.keys - desired.keys).forEach { id -> loaded.remove(id)?.close() }

        val errors = mutableMapOf<String, String>()
        val infos = mutableListOf<SourceRuntimeInfo>()
        for (record in records) {
            val cached = loaded[record.id]
            if (cached != null && cached.version == record.version) {
                infos += cached.info
                continue
            }
            cached?.close()
            loaded.remove(record.id)

            runCatching {
                val scriptFile = File(record.scriptPath)
                val script = buildString {
                    val config = record.config
                    append("globalThis.__MIYOMU_CONFIG = ")
                    append(config?.toString() ?: "{}")
                    append(";\n")
                    append(scriptFile.readText())
                }
                val dataDir = Path(scriptFile.absoluteFile.parentFile.absolutePath)
                val isAnime = isAnimeSourceScript(script)
                val comic = if (isAnime) null else loadJsComicSource(
                    script = script,
                    dataDir = dataDir,
                    expectedKey = record.id,
                )
                val anime = if (isAnime) loadJsAnimeSource(
                    script = script,
                    dataDir = dataDir,
                    expectedKey = record.id,
                ) else null
                val descriptor = comic?.descriptor ?: anime?.descriptor ?: error("unreachable")
                LoadedSource(
                    version = record.version,
                    info = SourceRuntimeInfo(
                        id = record.id,
                        name = descriptor.name,
                        lang = record.lang,
                        version = record.version,
                        mediaType = descriptor.mediaType.name,
                    ),
                    comic = comic,
                    anime = anime,
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

    override suspend fun search(sourceId: String, query: String, page: Int): Paged<ComicSummary> {
        val entry = entry(sourceId)
        entry.comic?.let { return it.search(query, page) }
        entry.anime?.let { return it.search(query, page) }
        error("源不支持搜索：$sourceId")
    }

    override suspend fun details(sourceId: String, comicId: String): ComicDetails {
        val entry = entry(sourceId)
        entry.comic?.let { return it.details(comicId) }
        entry.anime?.let { return it.details(comicId) }
        error("源不支持详情：$sourceId")
    }

    override suspend fun pages(sourceId: String, comicId: String, chapterId: String): List<PageRef> =
        entry(sourceId).comic?.pages(comicId, chapterId)
            ?: error("源不支持漫画页面：$sourceId")

    override suspend fun resolveVideo(sourceId: String, episodeUrl: String): ResolvedVideo =
        entry(sourceId).anime?.resolve(episodeUrl)
            ?: error("源不支持视频解析：$sourceId")

    override suspend fun loadImage(
        sourceId: String,
        url: String,
        headers: Map<String, String>,
    ): ByteArray {
        val entry = entry(sourceId)
        entry.comic?.let { return it.loadImage(url, headers) }
        entry.anime?.let { return it.loadImage(url, headers) }
        error("源未加载：$sourceId")
    }

    override fun close() {
        closed = true
        collectionJob.cancel()
        loaded.values.forEach { it.close() }
        loaded.clear()
        _state.value = SourceRuntimeState()
    }

    private fun entry(sourceId: String): LoadedSource =
        loaded[sourceId] ?: error("源未加载：$sourceId")
}
