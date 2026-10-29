package app.miyomu.source.runtime

import app.miyomu.source.ManagedAnimeSource
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
import kotlin.test.assertTrue

/**
 * Verifies the AppleCMS template source against a fixture server that speaks the real
 * /api.php/provide/vod/ JSON shapes (ADR-0014 D3).
 */
class AppleCmsTemplateSourceTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var baseUrl: String

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun startServer() {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "miyomu-applecms-test-http").apply { isDaemon = true }
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/api.php/provide/vod", ::handleVodApi)
            this.executor = this@AppleCmsTemplateSourceTest.executor
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
    fun searchesDetailsAndResolvesThroughTemplate() = runBlocking {
        val source = loadSource()
        try {
            assertEquals("applecms_demo", source.descriptor.id)
            assertEquals("AppleCMS Demo", source.descriptor.name)

            val search = source.search("海贼", page = 1)
            assertEquals(2, search.items.size)
            assertEquals("Demo Vod", search.items.first().title)
            assertEquals("更新至第2集", search.items.first().author)

            val details = source.details("1")
            assertEquals("Demo Vod", details.title)
            assertEquals("Demo content", details.description)
            assertEquals(listOf("动漫"), details.tags["type"])
            // m3u8 line first, then the web line; 2 + 2 episodes, numbers parsed from names.
            assertEquals(4, details.episodes.size)
            assertEquals(listOf(0, 0, 1, 1), details.episodes.map { it.line })
            assertEquals(listOf(1.0, 2.0, 1.0, 2.0), details.episodes.map { it.number })
            assertEquals(
                "$baseUrl/media/e1.m3u8",
                details.episodes.first().url,
            )

            val resolved = source.resolve(details.episodes.first().url)
            assertEquals(listOf("$baseUrl/media/e1.m3u8"), resolved.streams.map { it.url })
            assertEquals("hls", resolved.streams.single().type)
            assertEquals("Mozilla/5.0 (Miyomu AnimeSource)", resolved.headers["User-Agent"])

            // Non-m3u8 line resolves as mp4.
            val webResolved = source.resolve(details.episodes.last().url)
            assertEquals("mp4", webResolved.streams.single().type)
        } finally {
            source.close()
        }
    }

    @Test
    fun reportsHasNextFromPagecount() = runBlocking {
        val source = loadSource()
        try {
            val search = source.search("海贼", page = 1)
            assertTrue(search.hasNext)
        } finally {
            source.close()
        }
    }

    private suspend fun loadSource(): ManagedAnimeSource {
        val script = buildString {
            append("globalThis.__BASE_URL = ")
            append(json.encodeToString(baseUrl))
            append(";\n")
            append(resource("/sources/applecms_template.js"))
        }
        return loadJsAnimeSource(
            script = script,
            dataDir = Path(tempDir.resolve("applecms").toString()),
        )
    }

    private fun handleVodApi(exchange: HttpExchange) {
        val url = exchange.requestURI.toString()
        val body = when {
            "wd=" in url -> """
                {"code":1,"page":1,"pagecount":2,"limit":"20","total":3,"list":[
                  {"vod_id":1,"vod_name":"Demo Vod","vod_pic":"$baseUrl/pic/1.jpg","vod_remarks":"更新至第2集",
                   "vod_class":"动漫","vod_time":"2026-10-29","vod_content":"Demo content",
                   "vod_play_from":"dkm3u8__DOLLAR____DOLLAR____DOLLAR__webplay",
                   "vod_play_url":"第01集${'$'}$baseUrl/media/e1.m3u8#第02集${'$'}$baseUrl/media/e2.m3u8__DOLLAR____DOLLAR____DOLLAR__第01集${'$'}$baseUrl/media/e1.mp4#第02集${'$'}$baseUrl/media/e2.mp4"},
                  {"vod_id":2,"vod_name":"Second Vod","vod_pic":"","vod_remarks":"完结"}
                ]}
            """.trimIndent()
            "ids=1" in url -> """
                {"code":1,"page":1,"pagecount":1,"total":1,"list":[
                  {"vod_id":1,"vod_name":"Demo Vod","vod_pic":"$baseUrl/pic/1.jpg","vod_remarks":"更新至第2集",
                   "vod_class":"动漫","vod_time":"2026-10-29","vod_content":"Demo content",
                   "vod_play_from":"dkm3u8__DOLLAR____DOLLAR____DOLLAR__webplay",
                   "vod_play_url":"第01集${'$'}$baseUrl/media/e1.m3u8#第02集${'$'}$baseUrl/media/e2.m3u8__DOLLAR____DOLLAR____DOLLAR__第01集${'$'}$baseUrl/media/e1.mp4#第02集${'$'}$baseUrl/media/e2.mp4"}
                ]}
            """.trimIndent()
            else -> """{"code":1,"list":[]}"""
        }
        val resolved = body.replace("__DOLLAR__", "$")
        val bytes = resolved.encodeToByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun resource(path: String): String =
        AppleCmsTemplateSourceTest::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Missing test resource: $path")
}
