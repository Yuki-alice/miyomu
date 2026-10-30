package app.miyomu.source.runtime

import app.miyomu.network.createMiyomuHttpClient
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One QuickJS instance plus the host bindings every source kind shares (HTTP, KV, DOM bridge)
 * and the guarded invoke path. Source adapters own the preamble and action dispatch.
 */
internal class JsSourceRuntime(
    private val httpClient: HttpClient,
    private val keyValueStore: FileSourceKeyValueStore,
    private val dom: HtmlDomBridge,
    private val convert: ConvertBridge,
    private val timeoutMillis: Long,
    private val maxResponseBytes: Long,
    private val maxImageBytes: Int,
    private val memoryLimitBytes: Long,
) : AutoCloseable {

    private val quickJs: QuickJs = QuickJs.create(jobDispatcher = Dispatchers.Default)
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    private val mutex = Mutex()
    private var closed = false
    private val pendingBinaryResponses = mutableMapOf<String, ByteArray>()
    private var binaryResponseToken = 0L

    init {
        quickJs.evaluationTimeoutMillis = timeoutMillis
        quickJs.memoryLimit = memoryLimitBytes
        installHostBindings()
    }

    suspend fun evaluate(script: String) {
        quickJs.evaluate<Any?>(script)
    }

    suspend fun evaluateString(expression: String): String = quickJs.evaluate<String>(expression)

    suspend fun flushKeyValueStore() {
        keyValueStore.flush()
    }

    suspend fun invoke(action: String, payloadJson: String, sourceKey: String): String {
        quickJs.evaluate<Any?>("globalThis.__miyomuArgs = $payloadJson")
        return quickJs.evaluate<String>(
            "await __miyomuInvoke(${jsString(action)}, ${jsString(sourceKey)}, JSON.stringify(globalThis.__miyomuArgs))",
        )
    }

    suspend fun <T> guarded(action: String, block: suspend () -> T): T = mutex.withLock {
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

    suspend fun loadImage(url: String, headers: Map<String, String>): ByteArray {
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
        return bytes
    }

    override fun close() {
        if (closed) return
        closed = true
        pendingBinaryResponses.clear()
        runCatching { dom.close() }
        runCatching { quickJs.close() }
        runCatching { httpClient.close() }
    }

    private fun installHostBindings() {
        with(quickJs) {
            function<Unit>("__hostLog") { args ->
                println("[source] ${args.joinToString(" ")}")
            }

            function<String>("__hostConvertText") { args ->
                convertToText(
                    op = args.string(0),
                    value = args.getOrNull(1),
                    key = args.getOrNull(2),
                    iv = args.getOrNull(3),
                )
            }

            function<ByteArray>("__hostConvertBytes") { args ->
                val op = args.string(0)
                try {
                    convertToBytes(op, args.getOrNull(1), args.getOrNull(2), args.getOrNull(3))
                } catch (error: Throwable) {
                    throw IllegalArgumentException("Convert.$op failed: ${error.message}", error)
                }
            }

            asyncFunction<String>("__hostHttpRequest") { args ->
                httpRequest(
                    method = args.string(0),
                    url = args.string(1),
                    headersJson = args.getOrNull(2) as? String,
                    data = args.getOrNull(3) as? String,
                )
            }

            asyncFunction<String>("__hostHttpRequestBytes") { args ->
                httpRequestBytes(
                    method = args.string(0),
                    url = args.string(1),
                    headersJson = args.getOrNull(2) as? String,
                    data = args.getOrNull(3),
                )
            }
            function<ByteArray>("__hostHttpTakeBytes") { args ->
                pendingBinaryResponses.remove(args.string(0)) ?: ByteArray(0)
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

    private suspend fun httpRequestBytes(
        method: String,
        url: String,
        headersJson: String?,
        data: Any?,
    ): String {
        if (url.isBlank()) throw IllegalArgumentException("request url is empty")

        val requestHeaders = parseHeaders(headersJson)
        val response = httpClient.request(url) {
            this.method = HttpMethod.parse(method.uppercase())
            requestHeaders.forEach { (name, value) -> header(name, value) }
            when (data) {
                null -> Unit
                is String -> setBody(data)
                else -> setBody(data.convertBytes())
            }
        }

        val bytes = response.body<ByteArray>()
        if (bytes.size.toLong() > maxResponseBytes) {
            throw IllegalStateException(
                "response body exceeds $maxResponseBytes bytes for $url",
            )
        }
        val responseHeaders = response.headers.entries()
            .associate { entry -> entry.key.lowercase() to entry.value.joinToString(",") }

        binaryResponseToken += 1
        val token = binaryResponseToken.toString()
        pendingBinaryResponses[token] = bytes
        while (pendingBinaryResponses.size > PendingBinaryResponseLimit) {
            pendingBinaryResponses.remove(pendingBinaryResponses.keys.first())
        }
        return json.encodeToString(
            HostHttpBytesMeta(
                status = response.status.value,
                headers = responseHeaders,
                token = token,
            ),
        )
    }

    private fun parseHeaders(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }
            .getOrDefault(emptyMap())
    }

    /** `Convert` operations that hand text back to JS (hex digests, base64, decoded strings). */
    private fun convertToText(op: String, value: Any?, key: Any?, iv: Any?): String = when (op) {
        "utf8:decode" -> convert.utf8Decode(value.convertBytes())
        "gbk:decode" -> convert.gbkDecode(value.convertBytes())
        "base64:encode" -> convert.base64Encode(value.convertBytes())
        "hex:encode" -> convert.hexEncode(value.convertBytes())
        else -> if (op.startsWith("digest:")) {
            convert.hexEncode(convert.digest(op.substringAfter(':'), value.convertBytes()))
        } else if (op.startsWith("hmac-string:")) {
            convert.hexEncode(convert.hmac(op.substringAfter(':'), key.convertBytes(), value.convertBytes()))
        } else {
            throw IllegalArgumentException("unsupported Convert text op: $op")
        }
    }

    /** `Convert` operations that hand binary back to JS (encoded strings, digests, AES). */
    private fun convertToBytes(op: String, value: Any?, key: Any?, iv: Any?): ByteArray = when (op) {
        "utf8:encode" -> convert.utf8Encode(value.convertText())
        "gbk:encode" -> convert.gbkEncode(value.convertText())
        "base64:decode" -> convert.base64Decode(value.convertText())
        else -> when {
            op.startsWith("digest-raw:") ->
                convert.digest(op.substringAfter(':'), value.convertBytes())
            op.startsWith("hmac:") ->
                convert.hmac(op.substringAfter(':'), key.convertBytes(), value.convertBytes())
            op.startsWith("aes-") -> {
                val mode = op.substringAfter("aes-").substringBefore(':')
                val encrypt = op.substringAfter(':') == "enc"
                convert.aes(mode, encrypt, value.convertBytes(), key.convertBytes(), iv?.convertBytes())
            }
            else -> throw IllegalArgumentException("unsupported Convert binary op: $op")
        }
    }

    private fun Array<Any?>.string(index: Int): String =
        (getOrNull(index) as? String).orEmpty()

    private fun Array<Any?>.int(index: Int): Int =
        (getOrNull(index) as? Number)?.toInt() ?: -1

    companion object {
        private const val PendingBinaryResponseLimit = 8

        fun jsString(value: String): String = buildString {
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
    }
}
