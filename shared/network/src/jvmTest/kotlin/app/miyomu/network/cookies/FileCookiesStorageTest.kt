package app.miyomu.network.cookies

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileCookiesStorageTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    private val url = Url("https://source.example.com/manga/1")

    @Test
    fun persistsAndReloadsCookies() = runBlocking {
        val file = jarPath()
        val clock = AtomicLong(1_700_000_000_000)

        val first = FileCookiesStorage(file) { clock.get() }
        first.addCookie(url, Cookie("session", "abc123", maxAge = 3_600, path = "/", httpOnly = true))
        assertEquals("abc123", first.get(url).single().value)

        val second = FileCookiesStorage(file) { clock.get() }
        val restored = second.get(url).single()
        assertEquals("abc123", restored.value)
        assertTrue(restored.httpOnly)
    }

    @Test
    fun dropsCookieAfterMaxAge() = runBlocking {
        val file = jarPath()
        val clock = AtomicLong(1_700_000_000_000)
        val storage = FileCookiesStorage(file) { clock.get() }

        storage.addCookie(url, Cookie("session", "abc123", maxAge = 60, path = "/"))
        assertEquals(1, storage.get(url).size)

        clock.addAndGet(60_000)
        assertTrue(storage.get(url).isEmpty())
    }

    @Test
    fun deletionCookieRemovesStoredCookie() = runBlocking {
        val file = jarPath()
        val clock = AtomicLong(1_700_000_000_000)
        val storage = FileCookiesStorage(file) { clock.get() }

        storage.addCookie(url, Cookie("session", "abc123", maxAge = 3_600, path = "/"))
        storage.addCookie(url, Cookie("session", "", maxAge = 0, path = "/"))
        assertTrue(storage.get(url).isEmpty())

        val reloaded = FileCookiesStorage(file) { clock.get() }
        assertTrue(reloaded.get(url).isEmpty())
    }

    @Test
    fun isolatesCookiesByHost() = runBlocking {
        val storage = FileCookiesStorage(jarPath())

        storage.addCookie(url, Cookie("session", "abc123", maxAge = 3_600, path = "/"))
        assertTrue(storage.get(Url("https://other.example.org/")).isEmpty())
        assertEquals(1, storage.get(url).size)
    }

    @Test
    fun recoversFromCorruptJar() = runBlocking {
        val file = jarPath()
        Files.writeString(java.nio.file.Path.of(file.toString()), "{not json")
        val storage = FileCookiesStorage(file)

        assertTrue(storage.get(url).isEmpty())
        storage.addCookie(url, Cookie("session", "abc123", path = "/"))
        assertEquals("abc123", storage.get(url).single().value)
        assertTrue(SystemFileSystem.exists(Path(file.toString() + ".corrupt")))
    }

    @Test
    fun writesJarToDisk() = runBlocking {
        val file = jarPath()
        val storage = FileCookiesStorage(file)

        assertFalse(SystemFileSystem.exists(file))
        storage.addCookie(url, Cookie("session", "abc123", path = "/"))
        assertTrue(SystemFileSystem.exists(file))
        assertTrue(Files.size(java.nio.file.Path.of(file.toString())) > 0)
    }

    private fun jarPath(): Path = Path(tempDir.resolve("cookies.json").toString())
}
