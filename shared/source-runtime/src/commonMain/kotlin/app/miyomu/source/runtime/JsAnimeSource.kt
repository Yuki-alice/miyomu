package app.miyomu.source.runtime

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.MediaType
import app.miyomu.model.Paged
import app.miyomu.model.ResolvedVideo
import app.miyomu.model.SourceDescriptor
import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.network.cookies.FileCookiesStorage
import app.miyomu.source.ManagedAnimeSource
import app.miyomu.source.SourceException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.CookiesStorage
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal val AnimeSourceClassRegex =
    Regex("""(?m)^\s*class\s+([A-Za-z_$][A-Za-z0-9_$]*)\s+extends\s+AnimeSource\b""")

/** True when the script declares a class extending AnimeSource (decides the loader in the manager). */
fun isAnimeSourceScript(script: String): Boolean = AnimeSourceClassRegex.containsMatchIn(script)

private val AnimeSourceKeyRegex = Regex("""^[a-zA-Z_][a-zA-Z0-9_]*$""")

suspend fun loadJsAnimeSource(
    script: String,
    dataDir: Path,
    expectedKey: String? = null,
    timeoutMillis: Long = DefaultSourceTimeoutMillis,
    maxResponseBytes: Long = DefaultSourceMaxResponseBytes,
    maxImageBytes: Int = DefaultSourceMaxImageBytes,
    memoryLimitBytes: Long = DefaultSourceMemoryLimitBytes,
    httpClientFactory: (CookiesStorage) -> HttpClient = { createMiyomuHttpClient(it) },
): ManagedAnimeSource {
    SystemFileSystem.createDirectories(dataDir)

    val cookies = FileCookiesStorage(Path(dataDir, "cookies.json"))
    val httpClient = httpClientFactory(cookies)
    val keyValueStore = FileSourceKeyValueStore(Path(dataDir, "kv.json"))

    try {
        keyValueStore.open()
        val runtime = JsSourceRuntime(
            httpClient = httpClient,
            keyValueStore = keyValueStore,
            dom = createHtmlDomBridge(maxDocuments = 16),
            timeoutMillis = timeoutMillis,
            maxResponseBytes = maxResponseBytes,
            maxImageBytes = maxImageBytes,
            memoryLimitBytes = memoryLimitBytes,
        )
        val source = JsAnimeSource(runtime)
        try {
            source.initialize(script, expectedKey)
        } catch (error: Throwable) {
            runtime.close()
            throw error
        }
        return source
    } catch (error: Throwable) {
        httpClient.close()
        throw error
    }
}

internal class JsAnimeSource(
    private val runtime: JsSourceRuntime,
) : ManagedAnimeSource {

    private var sourceDescriptor: SourceDescriptor? = null
    private var closed = false

    override val descriptor: SourceDescriptor
        get() = sourceDescriptor ?: error("source is not initialized")

    internal suspend fun initialize(script: String, expectedKey: String?) {
        val className = AnimeSourceClassRegex.find(script)?.groupValues?.get(1)
            ?: throw SourceException("source script must declare a class extending AnimeSource")

        runtime.guarded("initialize") {
            runtime.evaluate(SourcePreamble)
            runtime.evaluate(script)

            val metaJson = runtime.evaluateString(
                "await __miyomuCreateSource(new $className())",
            )
            val meta = runtime.json.decodeFromString<SourceMetaDto>(metaJson)
            if (!AnimeSourceKeyRegex.matches(meta.key)) {
                throw SourceException("source key is invalid: ${meta.key}")
            }
            if (expectedKey != null && expectedKey != meta.key) {
                throw SourceException("source key ${meta.key} does not match expected key $expectedKey")
            }
            if (meta.name.isBlank()) throw SourceException("source name is required")
            if (meta.version.isBlank()) throw SourceException("source version is required")

            sourceDescriptor = SourceDescriptor(
                id = meta.key,
                name = meta.name,
                mediaType = MediaType.VIDEO,
                version = meta.version,
                apiVersion = 1,
                minAppVersion = meta.minAppVersion,
                url = meta.url,
            )
            runtime.flushKeyValueStore()
        }
    }

    override suspend fun search(query: String, page: Int): Paged<ComicSummary> =
        runtime.guarded("search") {
            val raw = runtime.invoke(
                "animeSearch",
                runtime.json.encodeToString(SearchPayload(query = query, page = page)),
                descriptor.id,
            )
            val dto = runtime.json.decodeFromString<JsAnimeListDto>(raw)
            runtime.flushKeyValueStore()

            val hasNext = dto.next != null || (dto.maxPage?.let { page < it } ?: false)
            Paged(
                items = dto.list.map { it.toSummary() },
                hasNext = hasNext,
            )
        }

    override suspend fun details(animeId: String): ComicDetails = runtime.guarded("details") {
        val raw = runtime.invoke(
            "animeDetail",
            runtime.json.encodeToString(DetailsPayload(id = animeId)),
            descriptor.id,
        )
        val dto = runtime.json.decodeFromString<JsAnimeDetailDto>(raw)
        runtime.flushKeyValueStore()
        dto.toDetails(animeId)
    }

    override suspend fun resolve(episodeUrl: String): ResolvedVideo = runtime.guarded("resolve") {
        val raw = runtime.invoke(
            "animeResolve",
            runtime.json.encodeToString(AnimeResolvePayload(episodeUrl = episodeUrl)),
            descriptor.id,
        )
        val dto = runtime.json.decodeFromString<JsAnimeResolveDto>(raw)
        runtime.flushKeyValueStore()
        dto.toResolvedVideo()
    }

    override suspend fun loadImage(url: String, headers: Map<String, String>): ByteArray =
        runtime.guarded("image") {
            runtime.loadImage(url, headers)
        }

    override fun close() {
        if (closed) return
        closed = true
        runtime.close()
    }
}
