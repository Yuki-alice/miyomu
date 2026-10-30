package app.miyomu.network

import io.ktor.client.HttpClient
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.Test as KTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebDavClientTest {

    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService
    private lateinit var baseUrl: String

    private val received = CopyOnWriteArrayList<Pair<String, String?>>() // method to auth header
    private var stored: ByteArray = ByteArray(0)

    @BeforeEach
    fun startServer() {
        executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "miyomu-webdav-test").apply { isDaemon = true }
        }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/dav/backup.json", ::handleBackup)
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

    @KTest
    fun uploadThenDownloadRoundTrip() = runBlocking {
        val httpClient = HttpClient()
        val client = WebDavClient(httpClient)
        try {
            client.upload("$baseUrl/dav/backup.json", "user", "pass", byteArrayOf(1, 2, 3))
            assertEquals("Basic dXNlcjpwYXNz", received.last().second)
            val downloaded = client.download("$baseUrl/dav/backup.json", "user", "pass")
            assertEquals(listOf<Byte>(1, 2, 3), downloaded.toList())
            assertTrue(client.exists("$baseUrl/dav/backup.json", "user", "pass"))
        } finally {
            httpClient.close()
        }
    }

    @KTest
    fun downloadReportsMissingRemote() = runBlocking {
        val httpClient = HttpClient()
        val client = WebDavClient(httpClient)
        try {
            // P1-5: 404 is now a typed WebDavNotFoundException (first-run semantics).
            val error = assertFailsWith<WebDavNotFoundException> {
                client.download("$baseUrl/dav/backup.json?empty=1", "user", "pass")
            }
            assertTrue(error.message.orEmpty().contains("does not exist"))
        } finally {
            httpClient.close()
        }
    }

    private fun handleBackup(exchange: HttpExchange) {
        received += exchange.requestMethod to exchange.requestHeaders.getFirst("Authorization")
        if (exchange.requestMethod == "PUT") {
            stored = exchange.requestBody.readBytes()
            exchange.sendResponseHeaders(201, -1)
            exchange.close()
            return
        }
        if (exchange.requestURI.query?.contains("empty=1") == true) {
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
            return
        }
        val bytes = if (stored.isEmpty()) "not-found".encodeToByteArray() else stored
        val status = if (stored.isEmpty()) 404 else 200
        exchange.sendResponseHeaders(status, if (status == 200) bytes.size.toLong() else -1)
        if (status == 200) exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }
}
