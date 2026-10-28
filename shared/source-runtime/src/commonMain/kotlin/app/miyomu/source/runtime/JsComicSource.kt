package app.miyomu.source.runtime

import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.MediaType
import app.miyomu.model.PageRef
import app.miyomu.model.Paged
import app.miyomu.model.SourceDescriptor
import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.network.cookies.FileCookiesStorage
import app.miyomu.source.ComicSource
import app.miyomu.source.ManagedComicSource
import app.miyomu.source.SourceException
import app.miyomu.source.SourceTimeoutException
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DefaultTimeoutMillis = 30_000L
private const val DefaultMaxResponseBytes = 8L * 1024 * 1024
private const val DefaultMaxImageBytes = 20 * 1024 * 1024
private const val DefaultMemoryLimitBytes = 64L * 1024 * 1024

private val SourceClassRegex =
    Regex("""(?m)^\s*class\s+([A-Za-z_$][A-Za-z0-9_$]*)\s+extends\s+ComicSource\b""")

private val SourceKeyRegex = Regex("""^[a-zA-Z_][a-zA-Z0-9_]*$""")

suspend fun loadJsComicSource(
    script: String,
    dataDir: Path,
    expectedKey: String? = null,
    timeoutMillis: Long = DefaultTimeoutMillis,
    maxResponseBytes: Long = DefaultMaxResponseBytes,
    maxImageBytes: Int = DefaultMaxImageBytes,
    memoryLimitBytes: Long = DefaultMemoryLimitBytes,
    httpClientFactory: (CookiesStorage) -> HttpClient = { createMiyomuHttpClient(it) },
): ManagedComicSource {
    SystemFileSystem.createDirectories(dataDir)

    val cookies = FileCookiesStorage(Path(dataDir, "cookies.json"))
    val httpClient = httpClientFactory(cookies)
    val keyValueStore = FileSourceKeyValueStore(Path(dataDir, "kv.json"))

    try {
        keyValueStore.open()
        val source = JsComicSource(
            httpClient = httpClient,
            keyValueStore = keyValueStore,
            dom = createHtmlDomBridge(maxDocuments = 16),
            timeoutMillis = timeoutMillis,
            maxResponseBytes = maxResponseBytes,
            maxImageBytes = maxImageBytes,
            memoryLimitBytes = memoryLimitBytes,
        )
        try {
            source.initialize(script, expectedKey)
        } catch (error: Throwable) {
            source.close()
            throw error
        }
        return source
    } catch (error: Throwable) {
        httpClient.close()
        throw error
    }
}

private class JsComicSource(
    private val httpClient: HttpClient,
    private val keyValueStore: FileSourceKeyValueStore,
    private val dom: HtmlDomBridge,
    private val timeoutMillis: Long,
    private val maxResponseBytes: Long,
    private val maxImageBytes: Int,
    private val memoryLimitBytes: Long,
) : ManagedComicSource {

    private val quickJs = QuickJs.create(jobDispatcher = Dispatchers.Default)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    private val mutex = Mutex()

    private var sourceDescriptor: SourceDescriptor? = null
    private var closed = false

    override val descriptor: SourceDescriptor
        get() = sourceDescriptor ?: error("source is not initialized")

    init {
        quickJs.evaluationTimeoutMillis = timeoutMillis
        quickJs.memoryLimit = memoryLimitBytes
        installHostBindings()
    }

    internal suspend fun initialize(script: String, expectedKey: String?) {
        val className = SourceClassRegex.find(script)?.groupValues?.get(1)
            ?: throw SourceException("source script must declare a class extending ComicSource")

        guarded("initialize") {
            quickJs.evaluate<Any?>(SourcePreamble)
            quickJs.evaluate<Any?>(script)

            val metaJson = quickJs.evaluate<String>(
                "await __miyomuCreateSource(new $className())",
            )
            val meta = json.decodeFromString<SourceMetaDto>(metaJson)
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
            keyValueStore.flush()
        }
    }

    override suspend fun search(query: String, page: Int, options: List<String>): Paged<ComicSummary> =
        guarded("search") {
            val raw = invoke("search", json.encodeToString(SearchPayload(query = query, page = page)))
            val dto = json.decodeFromString<JsComicListDto>(raw)
            keyValueStore.flush()

            val hasNext = dto.next != null || (dto.maxPage?.let { page < it } ?: false)
            Paged(
                items = dto.comics.map { it.toSummary() },
                hasNext = hasNext,
            )
        }

    override suspend fun details(comicId: String): ComicDetails = guarded("details") {
        val raw = invoke("details", json.encodeToString(DetailsPayload(id = comicId)))
        val dto = json.decodeFromString<JsComicDetailsDto>(raw)
        keyValueStore.flush()
        dto.toDetails(comicId)
    }

    override suspend fun pages(comicId: String, chapterId: String?): List<PageRef> =
        guarded("pages") {
            val raw = invoke(
                "pages",
                json.encodeToString(PagesPayload(comicId = comicId, chapterId = chapterId)),
            )
            val dto = json.decodeFromString<JsPagesDto>(raw)
            keyValueStore.flush()
            dto.toPages()
        }

    override suspend fun loadImage(url: String, headers: Map<String, String>): ByteArray =
        guarded("image") {
            if (url.isBlank()) throw SourceException("image url is empty")
            val response = httpClient.get(url) {
                headers.forEach { (name, value) -> header(name, value) }
            }
            if (!response.status.isSuccess()) {
                throw SourceException("image HTTP ${response.status.value} for $url")
            }
            val bytes = response.body<ByteArray>()
            if (bytes.size > maxImageBytes) {
                throw SourceException("image exceeds $maxImageBytes bytes: $url")
            }
            bytes
        }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { dom.close() }
        runCatching { quickJs.close() }
        runCatching { httpClient.close() }
    }

    private suspend fun invoke(action: String, payloadJson: String): String {
        quickJs.evaluate<Any?>("globalThis.__miyomuArgs = $payloadJson")
        return quickJs.evaluate<String>(
            "await __miyomuInvoke(${jsString(action)}, ${jsString(descriptor.id)}, JSON.stringify(globalThis.__miyomuArgs))",
        )
    }

    private suspend fun <T> guarded(action: String, block: suspend () -> T): T = mutex.withLock {
        if (closed) throw SourceException("source is closed")
        try {
            withTimeout(timeoutMillis) { block() }
        } catch (error: TimeoutCancellationException) {
            throw SourceTimeoutException(
                "source $action timed out after ${timeoutMillis}ms",
                error,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: SourceException) {
            throw error
        } catch (error: Exception) {
            throw SourceException("source $action failed: ${error.message}", error)
        }
    }

    private fun installHostBindings() {
        with(quickJs) {
            function<Unit>("__hostLog") { args ->
                println("[source] ${args.joinToString(" ")}")
            }

            asyncFunction<String>("__hostHttpRequest") { args ->
                httpRequest(
                    method = args.string(0),
                    url = args.string(1),
                    headersJson = args.getOrNull(2) as? String,
                    data = args.getOrNull(3) as? String,
                )
            }

            function<String?>("__hostKvGet") { args ->
                keyValueStore.get(args.string(0))
            }
            function<Unit>("__hostKvSet") { args ->
                keyValueStore.set(args.string(0), args.string(1))
            }
            function<Unit>("__hostKvDelete") { args ->
                keyValueStore.delete(args.string(0))
            }

            function<Int>("__hostParseHtml") { args -> dom.parse(args.string(0)) }
            function<Int?>("__hostDocQuery") { args -> dom.docQuery(args.int(0), args.string(1)) }
            function<String>("__hostDocQueryAll") { args -> dom.docQueryAll(args.int(0), args.string(1)) }
            function<Int?>("__hostDocGetElementById") { args -> dom.docGetById(args.int(0), args.string(1)) }
            function<Int?>("__hostElementQuery") { args -> dom.elementQuery(args.int(0), args.string(1)) }
            function<String>("__hostElementQueryAll") { args -> dom.elementQueryAll(args.int(0), args.string(1)) }
            function<String>("__hostElementText") { args -> dom.text(args.int(0)) }
            function<String>("__hostElementHtml") { args -> dom.html(args.int(0)) }
            function<String>("__hostElementAttributes") { args -> dom.attributes(args.int(0)) }
            function<String>("__hostElementClassNames") { args -> dom.classNames(args.int(0)) }
            function<String>("__hostElementId") { args -> dom.id(args.int(0)) }
            function<String>("__hostElementLocalName") { args -> dom.localName(args.int(0)) }
            function<String>("__hostElementChildren") { args -> dom.children(args.int(0)) }
            function<Int?>("__hostElementParent") { args -> dom.parent(args.int(0)) }
            function<Int?>("__hostElementPreviousSibling") { args -> dom.previousSibling(args.int(0)) }
            function<Int?>("__hostElementNextSibling") { args -> dom.nextSibling(args.int(0)) }
            function<Unit>("__hostDisposeDoc") { args -> dom.dispose(args.int(0)) }
        }
    }

    private suspend fun httpRequest(
        method: String,
        url: String,
        headersJson: String?,
        data: String?,
    ): String {
        if (url.isBlank()) throw IllegalArgumentException("request url is empty")

        val requestHeaders = parseHeaders(headersJson)
        val response = httpClient.request(url) {
            this.method = HttpMethod.parse(method.uppercase())
            requestHeaders.forEach { (name, value) -> header(name, value) }
            if (data != null) setBody(data)
        }

        val body = response.bodyAsText()
        if (body.length.toLong() > maxResponseBytes) {
            throw IllegalStateException(
                "response body exceeds $maxResponseBytes characters for $url",
            )
        }
        val responseHeaders = response.headers.entries()
            .associate { entry -> entry.key to entry.value.joinToString(",") }

        return json.encodeToString(
            HostHttpResponse(
                status = response.status.value,
                headers = responseHeaders,
                body = body,
            ),
        )
    }

    private fun parseHeaders(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrDefault(emptyMap())
    }

    private fun Array<Any?>.string(index: Int): String =
        (getOrNull(index) as? String).orEmpty()

    private fun Array<Any?>.int(index: Int): Int =
        (getOrNull(index) as? Number)?.toInt() ?: -1
}

private fun jsString(value: String): String = buildString {
    append('"')
    for (character in value) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\u2028' -> append("\\u2028")
            '\u2029' -> append("\\u2029")
            else -> append(character)
        }
    }
    append('"')
}
