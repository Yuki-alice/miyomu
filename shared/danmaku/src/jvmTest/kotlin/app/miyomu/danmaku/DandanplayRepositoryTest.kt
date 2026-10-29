package app.miyomu.danmaku

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DandanplayRepositoryTest {

    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var baseUrl: String

    private val receivedSignatures = mutableListOf<Pair<String?, String?>>() // appId to signature

    @BeforeEach
    fun startServer() {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "miyomu-danmaku-test").apply { isDaemon = true }
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/api/v2/search/episodes", ::handleSearch)
            createContext("/api/v2/comment", ::handleComment)
            this.executor = executor
            start()
        }
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
        executor.shutdownNow()
    }

    private val credentials = DanmakuCredentials(appId = "test-app", appSecret = "test-secret")

    @BeforeTest
    fun resetReceived() {
        receivedSignatures.clear()
    }

    @Test
    fun searchReturnsFlattenedEpisodes() = runBlocking {
        val repo = repo()
        val episodes = repo.searchEpisodes("海贼王")
        assertEquals(2, episodes.size)
        assertEquals("30012", episodes.first().episodeId)
        assertEquals("海贼王", episodes.first().animeTitle)
        assertEquals("第 1 集 海贼王", episodes.first().episodeTitle)
        assertEquals("test-app", receivedSignatures.single().first)
    }

    @Test
    fun signatureMatchesHmacSha256Spec() = runBlocking {
        val repo = repo()
        repo.searchEpisodes("海贼王")
        val (appId, signature) = receivedSignatures.single()
        assertEquals("test-app", appId)
        val path = "/api/v2/search/episodes?anime=%E6%B5%B7%E8%B4%BC%E7%8E%8B"
        val expected = expectedSignature(credentials, path)
        assertEquals(expected, signature)
    }

    @Test
    fun loadTrackParsesCommentArray() = runBlocking {
        val repo = repo()
        val track = repo.loadTrack("30012")
        // The third comment has empty text and must be dropped.
        assertEquals(2, track.comments.size)
        assertEquals(1000L, track.comments[0].timeMs)
        assertEquals("前菜", track.comments[0].text)
        assertEquals(5, track.comments[1].mode) // top
        assertEquals(0x00FF00, track.comments[1].color)
        assertEquals(3500L, track.comments[1].timeMs)
        assertTrue(track.commentsBetween(0, 2000).size == 1)
    }

    @Test
    fun refusesRequestsWithoutCredentials() {
        val repo = DandanplayRepository(HttpClient(), DanmakuCredentials("", ""), baseUrl = baseUrl)
        val error = assertFailsWith<IllegalStateException> {
            runBlocking { repo.searchEpisodes("x") }
        }
        assertTrue(error.message.orEmpty().contains("未配置"))
    }

    private fun repo(now: Long = 1_700_000_000) = DandanplayRepository(
        httpClient = HttpClient(),
        initialCredentials = credentials,
        baseUrl = baseUrl,
        nowSeconds = { now },
    )

    private fun expectedSignature(credentials: DanmakuCredentials, path: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(credentials.appSecret.encodeToByteArray(), "HmacSHA256"))
        val data = credentials.appId + 1_700_000_000 + path
        return java.util.Base64.getEncoder().encodeToString(mac.doFinal(data.encodeToByteArray()))
    }

    private fun handleSearch(exchange: HttpExchange) {
        receivedSignatures += exchange.requestHeaders.getFirst("X-AppId") to
            exchange.requestHeaders.getFirst("X-Signature")
        val body = """
            {"code":0,"animes":[
              {"animeTitle":"海贼王","type":"tv","episodes":[
                {"episodeId":"30012","episodeTitle":"第 1 集 海贼王"},
                {"episodeId":"30013","episodeTitle":"第 2 集"}
              ]}
            ]}
        """.trimIndent()
        exchange.respondJson(body)
    }

    private fun handleComment(exchange: HttpExchange) {
        val body = """
            {"code":0,"count":3,"comments":[
              {"m":"前菜","p":"1.0,1,16777215,bilibili"},
              {"m":"顶","p":"3.5,5,65280,bilibili"},
              {"m":"","p":"12.0,1,16711680,bilibili"}
            ]}
        """.trimIndent()
        exchange.respondJson(body)
    }

    private fun HttpExchange.respondJson(body: String) {
        val bytes = body.encodeToByteArray()
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
