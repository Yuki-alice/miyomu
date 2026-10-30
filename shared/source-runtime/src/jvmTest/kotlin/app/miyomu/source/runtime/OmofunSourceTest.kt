package app.miyomu.source.runtime

import app.miyomu.source.ManagedAnimeSource
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Offline verification of the OmoFun source selectors against fixture HTML that mirrors the
 * live site's MacCMS markup. Live reachability is covered by [CanarySourceTest] (-Pcanary).
 *
 * Tagged `canary`: the suite reads the real `demo-resources/sources/anime/omofun.js` product
 * source, which is no longer committed (it was removed with the other verification artifacts),
 * so it only runs in checkouts that restore that fixture, via `-Pcanary`.
 */
@Tag("canary")
class OmofunSourceTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var baseUrl: String

    @BeforeEach
    fun startServer() {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "miyomu-omofun-test-http").apply { isDaemon = true }
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/vod/search.html", ::handleSearch)
            createContext("/vod/detail", ::handleDetail)
            createContext("/vod/play", ::handlePlay)
            createContext("/_dyn_plays", ::handleDynPlays)
            this.executor = this@OmofunSourceTest.executor
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
        val source = loadSource()
        try {
            assertEquals("omofun", source.descriptor.id)

            val search = source.search("海贼王", page = 1)
            assertEquals(1, search.items.size)
            assertEquals("Demo Anime", search.items.single().title)
            assertEquals("$baseUrl/vodimg/small/1.jpg", search.items.single().coverUrl)
            assertEquals("更新至第05集", search.items.single().author)

            val details = source.details("/vod/detail/2025314279.html")
            assertEquals("Demo Anime", details.title)
            assertEquals("Demo description", details.description)
            assertEquals(listOf("2025", "日本"), details.tags["type"])
            assertEquals(3, details.episodes.size)
            assertEquals(listOf("第01集", "第02集", "第03集"), details.episodes.map { it.name })
            assertEquals(listOf(1.0, 2.0, 3.0), details.episodes.map { it.number })
            assertEquals("/vod/play/2025314279/ep1.html", details.episodes.first().url)

            val resolved = source.resolve(details.episodes.first().url)
            assertEquals(
                listOf("$baseUrl/stream/1/index.m3u8", "$baseUrl/stream/1-mirror/index.m3u8"),
                resolved.streams.map { it.url },
            )
            assertEquals(listOf("ikzy", "mdzy"), resolved.streams.map { it.quality })
            assertEquals("hls", resolved.streams.first().type)
            assertEquals("$baseUrl/", resolved.headers["Referer"])
        } finally {
            source.close()
        }
    }

    @Test
    fun resolveFailsLoudlyWithoutStreams() {
        val error = runCatching {
            runBlocking {
                val source = loadSource()
                try {
                    source.resolve("/vod/play/999/ep1.html")
                } finally {
                    source.close()
                }
            }
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("no streams"))
    }

    private suspend fun loadSource(): ManagedAnimeSource {
        val script = realSource().replace("https://omofun.in", baseUrl)
        return loadJsAnimeSource(
            script = script,
            dataDir = Path(tempDir.resolve("omofun").toString()),
        )
    }

    /** The real product source (demo-resources/sources/anime/omofun.js) against the fixture server. */
    private fun realSource(): String =
        File("../..", "demo-resources/sources/anime/omofun.js").readText()

    private fun handleSearch(exchange: HttpExchange) {
        exchange.respondHtml(
            """
            <html><body>
              <div class="module-items module-card-items">
                <div class="module-card-item module-item">
                  <div class="module-card-item-class">日漫</div>
                  <a href="/vod/detail/2025314279.html" class="module-card-item-poster">
                    <div class="module-item-cover">
                      <div class="module-item-note">更新至第05集</div>
                      <div class="module-item-pic"><img class="lazy lazyload" data-original="/vodimg/small/1.jpg" src="/loading.gif"></div>
                    </div>
                  </a>
                  <div class="module-card-item-info">
                    <div class="module-card-item-title"><a href="/vod/detail/2025314279.html">Demo Anime</a></div>
                  </div>
                </div>
              </div>
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handleDetail(exchange: HttpExchange) {
        exchange.respondHtml(
            """
            <html><body>
              <div class="module-info-main">
                <div class="module-info-heading">
                  <h1>Demo Anime</h1>
                  <div class="module-info-tag">
                    <div class="module-info-tag-link"><a title="2025" href="#">2025</a></div>
                    <div class="module-info-tag-link"><a title="日本" href="#">日本</a></div>
                  </div>
                </div>
                <div class="module-info-pic"><img class="lazy lazyload" data-original="/vodimg/1.jpg" src="/loading.gif"></div>
              </div>
              <div class="module-info-introduction-content"><p>Demo description</p></div>
              <div class="module-play-list">
                <div class="module-play-list-content module-play-list-base">
                  <a class="module-play-list-link" href="/vod/play/2025314279/ep1.html" title="播放Demo Anime第01集"><span>第01集</span></a>
                  <a class="module-play-list-link" href="/vod/play/2025314279/ep2.html" title="播放Demo Anime第02集"><span>第02集</span></a>
                  <a class="module-play-list-link" href="/vod/play/2025314279/ep3.html" title="播放Demo Anime第03集"><span>第03集</span></a>
                </div>
              </div>
            </body></html>
            """.trimIndent(),
        )
    }

    private fun handlePlay(exchange: HttpExchange) {
        exchange.respondHtml("<html><body>play page</body></html>")
    }

    private fun handleDynPlays(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        val body = if (path.endsWith("/999/ep1")) {
            """{"video_plays":[]}"""
        } else {
            """
            {"video_plays":[
              {"play_data":"$baseUrl/stream/1/index.m3u8","src_site":"ikzy"},
              {"play_data":"$baseUrl/stream/1-mirror/index.m3u8","src_site":"mdzy"}
            ]}
            """.trimIndent()
        }
        val bytes = body.encodeToByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun HttpExchange.respondHtml(html: String) {
        val bytes = html.encodeToByteArray()
        responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
