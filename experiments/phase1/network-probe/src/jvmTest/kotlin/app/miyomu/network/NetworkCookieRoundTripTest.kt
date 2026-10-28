package app.miyomu.network

import app.miyomu.network.cookies.FileCookiesStorage
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkCookieRoundTripTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private lateinit var server: HttpServer
    private lateinit var baseUrl: String

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/login", ::handleLogin)
            createContext("/echo-cookie", ::handleEchoCookie)
            createContext("/echo-user-agent", ::handleEchoUserAgent)
            executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "miyomu-test-http").apply { isDaemon = true }
            }
            start()
        }
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun cookieSurvivesClientRestart() = runBlocking {
        val jar = Path(tempDir.resolve("cookies.json").toString())

        val first = createMiyomuHttpClient(FileCookiesStorage(jar))
        try {
            assertEquals("ok", first.get("$baseUrl/login").bodyAsText())
        } finally {
            first.close()
        }
        assertTrue(SystemFileSystem.exists(jar))

        val second = createMiyomuHttpClient(FileCookiesStorage(jar))
        val header = try {
            second.get("$baseUrl/echo-cookie").bodyAsText()
        } finally {
            second.close()
        }

        assertEquals("miyomu_session=abc123", header)
    }

    @Test
    fun clientSendsUserAgent() = runBlocking {
        val client = createMiyomuHttpClient(FileCookiesStorage(Path(tempDir.resolve("ua.json").toString())))
        val agent = try {
            client.get("$baseUrl/echo-user-agent").bodyAsText()
        } finally {
            client.close()
        }
        assertEquals(MiyomuUserAgent, agent)
    }

    private fun handleLogin(exchange: HttpExchange) {
        exchange.responseHeaders.add("Set-Cookie", "miyomu_session=abc123; Path=/; HttpOnly; Max-Age=3600")
        exchange.respond("ok")
    }

    private fun handleEchoCookie(exchange: HttpExchange) {
        exchange.respond(exchange.requestHeaders.getFirst("Cookie") ?: "none")
    }

    private fun handleEchoUserAgent(exchange: HttpExchange) {
        exchange.respond(exchange.requestHeaders.getFirst("User-Agent") ?: "none")
    }

    private fun HttpExchange.respond(body: String) {
        val bytes = body.encodeToByteArray()
        responseHeaders.add("Content-Type", "text/plain")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }
}
