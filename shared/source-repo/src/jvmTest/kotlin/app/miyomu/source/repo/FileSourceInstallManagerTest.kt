package app.miyomu.source.repo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileSourceInstallManagerTest {

    @TempDir
    lateinit var root: File

    private val script = "class Demo extends ComicSource {\n  key = 'demo'\n}\n"
    private var indexJson = indexOf(version = "1.0")
    private var indexStatus = HttpStatusCode.OK

    private val engine = MockEngine { request ->
        when (request.url.toString()) {
            "https://repo.test/index.json" -> respond(
                content = indexJson,
                status = indexStatus,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
            "https://repo.test/demo.js" -> respond(
                content = script,
                headers = headersOf(HttpHeaders.ContentType, "text/javascript"),
            )
            else -> respond("missing", HttpStatusCode.NotFound)
        }
    }

    private fun manager(): FileSourceInstallManager =
        FileSourceInstallManager(
            baseDir = Path(File(root, "sources").absolutePath),
            httpClient = HttpClient(engine),
        )

    @Test
    fun `installs and updates a source from a relative URL`() = runBlocking {
        val manager = manager()
        manager.addRepository("https://repo.test/index.json")

        val catalog = manager.fetchCatalog()
        assertTrue(catalog.errors.isEmpty())
        val demo = catalog.available.single()
        assertEquals("demo", demo.packageInfo.id)
        assertFalse(demo.isInstalled)

        val installed = manager.install(demo)
        assertEquals("1.0", installed.version)
        assertEquals(script, File(installed.scriptPath).readText())
        assertEquals(sha256Hex(script.toByteArray()), installed.sha256)
        assertTrue(File(installed.scriptPath).isFile)
        assertTrue(File(root, "sources/manifest.json").isFile)

        indexJson = indexOf(version = "1.1")
        val update = manager.fetchCatalog().available.single()
        assertTrue(update.updateAvailable)

        val updated = manager.install(update)
        assertEquals("1.1", updated.version)
        assertEquals(installed.installedAt, updated.installedAt)
        assertEquals(1, manager.installed.value.size)
    }

    @Test
    fun `rejects a sha256 mismatch and writes nothing`() = runBlocking {
        indexJson = """
            {"sources":[{"id":"demo","name":"Demo","version":"1.0","url":"demo.js","sha256":"deadbeef"}]}
        """.trimIndent()
        val manager = manager()
        manager.addRepository("https://repo.test/index.json")
        val demo = manager.fetchCatalog().available.single()

        assertThrows<IllegalArgumentException> {
            runBlocking { manager.install(demo) }
        }
        assertTrue(manager.installed.value.isEmpty())
        assertFalse(File(root, "sources/sources/demo/source.js").exists())
    }

    @Test
    fun `rejects ids that escape the source directory`() {
        runBlocking {
            indexJson = """
                {"sources":[{"id":"../evil","name":"Evil","version":"1.0","url":"demo.js"}]}
            """.trimIndent()
            val manager = manager()
            manager.addRepository("https://repo.test/index.json")
            val evil = manager.fetchCatalog().available.single()

            assertThrows<IllegalArgumentException> {
                runBlocking { manager.install(evil) }
            }
        }
    }

    @Test
    fun `uninstall removes the record and the script`() = runBlocking {
        val manager = manager()
        manager.addRepository("https://repo.test/index.json")
        val installed = manager.install(manager.fetchCatalog().available.single())

        manager.uninstall(installed.id)

        assertTrue(manager.installed.value.isEmpty())
        assertFalse(File(installed.scriptPath).exists())
    }

    @Test
    fun `repositories and installs survive a new manager`() = runBlocking {
        val first = manager()
        first.addRepository("https://repo.test/index.json")
        first.install(first.fetchCatalog().available.single())

        val second = manager()
        second.ensureLoaded()

        assertEquals(listOf("https://repo.test/index.json"), second.repositories.value)
        assertEquals(listOf("demo"), second.installed.value.map { it.id })
        assertNotNull(second.getById("demo"))
    }

    @Test
    fun `a broken repository does not hide the others`() = runBlocking {
        val manager = manager()
        manager.addRepository("https://broken.test/index.json")
        manager.addRepository("https://repo.test/index.json")

        val catalog = manager.fetchCatalog()

        assertEquals(1, catalog.available.size)
        assertEquals(1, catalog.errors.size)
        assertTrue(catalog.errors.single().startsWith("https://broken.test/index.json"))
    }

    @Test
    fun `removing a repository keeps installed sources`() = runBlocking {
        val manager = manager()
        manager.addRepository("https://repo.test/index.json")
        manager.install(manager.fetchCatalog().available.single())

        manager.removeRepository("https://repo.test/index.json")

        assertTrue(manager.repositories.value.isEmpty())
        assertEquals(listOf("demo"), manager.installed.value.map { it.id })
        assertNull(manager.fetchCatalog().available.firstOrNull())
    }

    @Test
    fun `installs over a real local HTTP server`() {
        val server = HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/index.json") { exchange ->
            val body = indexOf(version = "1.0").toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/demo.js") { exchange ->
            val body = script.toByteArray()
            exchange.responseHeaders.add("Content-Type", "text/javascript")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            runBlocking {
                val manager = FileSourceInstallManager(Path(File(root, "http-sources").absolutePath))
                manager.addRepository("http://127.0.0.1:${server.address.port}/index.json")
                val installed = manager.install(manager.fetchCatalog().available.single())

                assertEquals(script, File(installed.scriptPath).readText())
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `resolves absolute and relative script urls`() {
        assertEquals(
            "https://repo.test/demo.js",
            resolveUrl("https://repo.test/index.json", "demo.js"),
        )
        assertEquals(
            "https://cdn.test/demo.js",
            resolveUrl("https://repo.test/index.json", "https://cdn.test/demo.js"),
        )
        assertEquals(
            "https://repo.test/repo/sources/demo.js",
            resolveUrl("https://repo.test/repo/index.json", "sources/demo.js"),
        )
        assertThrows<IllegalArgumentException> {
            resolveUrl("https://repo.test/index.json", "file:///etc/passwd")
        }
    }

    private fun indexOf(version: String): String = """
        {"name":"Test repo","version":1,"sources":[{"id":"demo","name":"Demo","version":"$version","lang":"en","url":"demo.js"}]}
    """.trimIndent()

    private fun sha256Hex(text: String): String = sha256Hex(text.toByteArray())
}
