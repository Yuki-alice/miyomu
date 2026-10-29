package app.miyomu.source.runtime

import app.miyomu.model.ResolvedVideo
import app.miyomu.source.ManagedAnimeSource
import app.miyomu.source.SourceException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsAnimeSourceTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var baseUrl: String

    private val receivedResolveTestHeaders = mutableListOf<String?>()

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun startServer() {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "miyomu-anime-source-test-http").apply { isDaemon = true }
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/anime/search", ::handleSearch)
            createContext("/anime/detail", ::handleDetail)
            createContext("/api/resolve", ::handleResolve)
            this.executor = this@JsAnimeSourceTest.executor
            start()
        }
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
        executor.shutdownNow()
    }

    @Test
    fun readsSearchDetailsAndResolve() = runBlocking {
        val source = loadSource("demo_anime_source", "anime")
        try {
            val search = source.search("demo", page = 1)
            assertEquals(2, search.items.size)
            assertEquals("Demo Anime", search.items.first().title)
            assertEquals("$baseUrl/cover1.jpg", search.items.first().coverUrl)
            assertEquals(false, search.hasNext)

            val details = source.details("demo")
            assertEquals("Demo Anime", details.title)
            assertEquals("Demo description", details.description)
            assertEquals(4, details.episodes.size)
            assertEquals(listOf("第 1 集", "第 2 集", "第 1 集", "第 2 集"), details.episodes.map { it.name })
            assertEquals(listOf(0, 0, 1, 1), details.episodes.map { it.line })
            assertEquals(listOf(1.0, 2.0, 1.0, 2.0), details.episodes.map { it.number })
            assertEquals(
                "$baseUrl/play/e1.m3u8",
                details.episodes.first().url,
            )

            val resolved = source.resolve(details.episodes.first().url)
            assertTrue(resolved is ResolvedVideo)
            assertEquals(
                listOf("$baseUrl/stream/e1/index.m3u8", "$baseUrl/stream/e1-720.m3u8"),
                resolved.streams.map { it.url },
            )
            assertEquals(listOf("1080P", "720P"), resolved.streams.map { it.quality })
            assertEquals("hls", resolved.streams.first().type)
            assertEquals("MiyomuAnimeTest/1.0", resolved.headers["User-Agent"])
            assertEquals("$baseUrl/", resolved.headers["Referer"])
            assertEquals("resolve", receivedResolveTestHeaders.single())
        } finally {
            source.close()
        }
    }

    @Test
    fun rejectsScriptsThatDoNotExtendAnimeSource() {
        val script = "class Wrong extends ComicSource { name = \"x\"; key = \"x\"; version = \"1\"; }"
        val error = assertFailsWith<SourceException> {
            runBlocking {
                loadSource(scriptText = script, directory = "wrong")
            }
        }
        assertTrue(error.message.orEmpty().contains("AnimeSource"))
    }

    private suspend fun loadSource(
        resourceName: String? = null,
        directory: String,
        scriptText: String? = null,
    ): ManagedAnimeSource {
        val script = buildString {
            append("globalThis.__BASE_URL = ")
            append(json.encodeToString(baseUrl))
            append(";\n")
            append(scriptText ?: resource("/sources/${requireNotNull(resourceName)}.js"))
        }
        return loadJsAnimeSource(
            script = script,
            dataDir = Path(tempDir.resolve(directory).toString()),
        )
    }

    private fun handleSearch(exchange: HttpExchange) {
        exchange.respondHtml(
            """
            <html><body>
              <a class="anime-item" data-id="demo">
                <span class="title">Demo Anime</span>
                <img data-src="$baseUrl/cover1.jpg">
              </a>
              <a class="anime-item" data-id="demo2">
                <span class="title">Second Anime</span>
              </a>
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handleDetail(exchange: HttpExchange) {
        exchange.respondHtml(
            """
            <html><body>
              <h1 class="title">Demo Anime</h1>
              <img data-src="$baseUrl/cover1.jpg">
              <p class="desc">Demo description</p>
              <div class="line">
                <a data-url="$baseUrl/play/e1.m3u8" data-number="1">第 1 集</a>
                <a data-url="$baseUrl/play/e2.m3u8" data-number="2">第 2 集</a>
              </div>
              <div class="line">
                <a data-url="$baseUrl/play/e1-b.m3u8" data-number="1">第 1 集</a>
                <a data-url="$baseUrl/play/e2-b.m3u8" data-number="2">第 2 集</a>
              </div>
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handleResolve(exchange: HttpExchange) {
        receivedResolveTestHeaders += exchange.requestHeaders.getFirst("X-Miyomu-Test")
        val body = """
            {"urls":[
              {"url":"$baseUrl/stream/e1/index.m3u8","quality":"1080P","type":"hls"},
              {"url":"$baseUrl/stream/e1-720.m3u8","quality":"720P","type":"hls"}
            ]}
        """.trimIndent()
        val bytes = body.encodeToByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun resource(path: String): String =
        JsAnimeSourceTest::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing test resource: $path")

    private fun HttpExchange.respondHtml(html: String) {
        val bytes = html.encodeToByteArray()
        responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
