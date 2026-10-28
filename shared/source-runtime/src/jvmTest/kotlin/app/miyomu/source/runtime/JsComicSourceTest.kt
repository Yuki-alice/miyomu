package app.miyomu.source.runtime

import app.miyomu.source.ManagedComicSource
import app.miyomu.source.SourceException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class JsComicSourceTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var baseUrl: String

    private val receivedCookies = CopyOnWriteArrayList<String?>()
    private val receivedTestHeaders = CopyOnWriteArrayList<String?>()

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun startServer() {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "miyomu-source-test-http").apply { isDaemon = true }
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/html/search", ::handleSearch)
            createContext("/html/comic", ::handleDetails)
            createContext("/html/chapter", ::handleChapter)
            createContext("/images", ::handleImage)
            this.executor = this@JsComicSourceTest.executor
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
    fun readsSearchDetailsAndPagesWithCookies() = runBlocking {
        val source = loadSource("html_fixture", "html", expectedKey = "html_fixture")
        try {
            val search = source.search("demo")
            assertEquals(2, search.items.size)
            assertEquals("Demo Comic", search.items.first().title)
            assertEquals("Demo Author", search.items.first().author)
            assertEquals(false, search.hasNext)

            val details = source.details("demo")
            assertEquals("Demo Comic", details.title)
            assertEquals(listOf("Chapter 1", "Chapter 2"), details.chapters.map { it.title })
            assertEquals(listOf("Demo Author"), details.tags["author"])

            val pages = source.pages("demo", details.chapters.first().id)
            assertEquals(
                listOf("$baseUrl/images/1.jpg", "$baseUrl/images/2.jpg"),
                pages.map { it.url },
            )

            val image = source.loadImage(pages.first().url, pages.first().headers)
            assertEquals(listOf<Byte>(1, 2, 3, 4), image.toList())

            assertEquals("search", receivedTestHeaders.single())
            assertEquals(4, receivedCookies.size)
            assertTrue(receivedCookies.drop(1).all { it?.contains("sid=abc123") == true })
        } finally {
            source.close()
        }
    }

    @Test
    fun persistsSourceKeyValueAcrossReloads() = runBlocking {
        val first = loadSource("kv_source", "kv", expectedKey = "kv_source")
        try {
            assertEquals("1", first.search("x").items.single().id)
        } finally {
            first.close()
        }

        val second = loadSource("kv_source", "kv", expectedKey = "kv_source")
        try {
            assertEquals("2", second.search("x").items.single().id)
        } finally {
            second.close()
        }
    }

    @Test
    fun rejectsMismatchedSourceKey() = runBlocking {
        val error = assertFailsWith<SourceException> {
            loadSource("html_fixture", "wrong-key", expectedKey = "other_source")
        }
        assertTrue(error.message.orEmpty().contains("does not match"))
    }

    @Test
    fun stopsInfiniteSourceInit() = runBlocking {
        val start = System.nanoTime()
        assertFailsWith<SourceException> {
            loadSource("loop_source", "loop", timeoutMillis = 500)
        }
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000
        assertTrue(elapsedMillis < 5_000, "source init took too long: $elapsedMillis ms")
    }

    private suspend fun loadSource(
        resourceName: String,
        directory: String,
        expectedKey: String? = null,
        timeoutMillis: Long = 30_000,
    ): ManagedComicSource {
        val script = buildString {
            append("globalThis.__BASE_URL = ")
            append(json.encodeToString(baseUrl))
            append(";\n")
            append(resource("/sources/$resourceName.js"))
        }
        return loadJsComicSource(
            script = script,
            dataDir = Path(tempDir.resolve(directory).toString()),
            expectedKey = expectedKey,
            timeoutMillis = timeoutMillis,
        )
    }

    private fun handleSearch(exchange: HttpExchange) {
        receivedTestHeaders += exchange.requestHeaders.getFirst("X-Miyomu-Test")
        receivedCookies += exchange.requestHeaders.getFirst("Cookie")
        exchange.responseHeaders.add("Set-Cookie", "sid=abc123; Path=/; HttpOnly; Max-Age=3600")
        exchange.respondHtml(
            """
            <html><body>
              <a class="comic" data-id="demo">
                <span class="title">Demo Comic</span>
                <span class="author">Demo Author</span>
              </a>
              <a class="comic" data-id="demo2">
                <span class="title">Second Comic</span>
              </a>
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handleDetails(exchange: HttpExchange) {
        receivedCookies += exchange.requestHeaders.getFirst("Cookie")
        exchange.respondHtml(
            """
            <html><body>
              <h1 class="title">Demo Comic</h1>
              <span class="author">Demo Author</span>
              <div class="chapters">
                <a data-id="1">Chapter 1</a>
                <a data-id="2">Chapter 2</a>
              </div>
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handleChapter(exchange: HttpExchange) {
        receivedCookies += exchange.requestHeaders.getFirst("Cookie")
        exchange.respondHtml(
            """
            <html><body>
              <img data-src="/images/1.jpg">
              <img data-src="/images/2.jpg">
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handleImage(exchange: HttpExchange) {
        receivedCookies += exchange.requestHeaders.getFirst("Cookie")
        val bytes = byteArrayOf(1, 2, 3, 4)
        exchange.responseHeaders.add("Content-Type", "image/png")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun resource(path: String): String =
        JsComicSourceTest::class.java.getResourceAsStream(path)
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
