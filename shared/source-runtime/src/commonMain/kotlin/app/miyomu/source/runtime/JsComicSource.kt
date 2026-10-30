package app.miyomu.source.runtime

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.MediaType
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.SourceDescriptor
import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.network.cookies.FileCookiesStorage
import app.miyomu.source.ManagedComicSource
import app.miyomu.source.SourceException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.CookiesStorage
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal const val DefaultSourceTimeoutMillis = 30_000L
internal const val DefaultSourceMaxResponseBytes = 8L * 1024 * 1024
internal const val DefaultSourceMaxImageBytes = 20 * 1024 * 1024
internal const val DefaultSourceMemoryLimitBytes = 64L * 1024 * 1024

internal val SourceClassRegex =
    Regex("""(?m)^\s*class\s+([A-Za-z_$][A-Za-z0-9_$]*)\s+extends\s+ComicSource\b""")

private val SourceKeyRegex = Regex("""^[a-zA-Z_][a-zA-Z0-9_]*$""")

suspend fun loadJsComicSource(
    script: String,
    dataDir: Path,
    expectedKey: String? = null,
    timeoutMillis: Long = DefaultSourceTimeoutMillis,
    maxResponseBytes: Long = DefaultSourceMaxResponseBytes,
    maxImageBytes: Int = DefaultSourceMaxImageBytes,
    memoryLimitBytes: Long = DefaultSourceMemoryLimitBytes,
    httpClientFactory: (CookiesStorage) -> HttpClient = { createMiyomuHttpClient(it) },
): ManagedComicSource {
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
            convert = createConvertBridge(),
            timeoutMillis = timeoutMillis,
            maxResponseBytes = maxResponseBytes,
            maxImageBytes = maxImageBytes,
            memoryLimitBytes = memoryLimitBytes,
        )
        val source = JsComicSource(runtime)
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

internal class JsComicSource(
    private val runtime: JsSourceRuntime,
) : ManagedComicSource {

    private var sourceDescriptor: SourceDescriptor? = null
    private var closed = false

    override val descriptor: SourceDescriptor
        get() = sourceDescriptor ?: error("source is not initialized")

    internal suspend fun initialize(script: String, expectedKey: String?) {
        val className = SourceClassRegex.find(script)?.groupValues?.get(1)
            ?: throw SourceException("source script must declare a class extending ComicSource")

        runtime.guarded("initialize") {
            runtime.evaluate(SourcePreamble)
            runtime.evaluate(script)

            val metaJson = runtime.evaluateString(
                "await __miyomuCreateSource(new $className())",
            )
            val meta = runtime.json.decodeFromString<SourceMetaDto>(metaJson)
            if (!SourceKeyRegex.matches(meta.key)) {
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
                mediaType = MediaType.COMIC,
                version = meta.version,
                apiVersion = 1,
                minAppVersion = meta.minAppVersion,
                url = meta.url,
            )
            runtime.flushKeyValueStore()
        }
    }

    override suspend fun search(query: String, page: Int, options: List<String>): Paged<ComicSummary> =
        runtime.guarded("search") {
            val raw = runtime.invoke(
                "search",
                runtime.json.encodeToString(SearchPayload(query = query, page = page)),
                descriptor.id,
            )
            val dto = runtime.json.decodeFromString<JsComicListDto>(raw)
            runtime.flushKeyValueStore()

            val hasNext = dto.next != null || (dto.maxPage?.let { page < it } ?: false)
            Paged(
                items = dto.comics.map { it.toSummary() },
                hasNext = hasNext,
            )
        }

    override suspend fun details(comicId: String): ComicDetails = runtime.guarded("details") {
        val raw = runtime.invoke(
            "details",
            runtime.json.encodeToString(DetailsPayload(id = comicId)),
            descriptor.id,
        )
        val dto = runtime.json.decodeFromString<JsComicDetailsDto>(raw)
        runtime.flushKeyValueStore()
        dto.toDetails(comicId)
    }

    override suspend fun pages(comicId: String, chapterId: String?): List<PageRef> =
        runtime.guarded("pages") {
            val raw = runtime.invoke(
                "pages",
                runtime.json.encodeToString(PagesPayload(comicId = comicId, chapterId = chapterId)),
                descriptor.id,
            )
            val dto = runtime.json.decodeFromString<JsPagesDto>(raw)
            runtime.flushKeyValueStore()
            dto.toPages()
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
