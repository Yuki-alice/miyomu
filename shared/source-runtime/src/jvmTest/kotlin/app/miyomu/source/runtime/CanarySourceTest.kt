package app.miyomu.source.runtime

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

/**
 * Live smoke test for the product sources in `demo-resources/sources/anime/`: search → detail →
 * resolve must produce at least one stream URL per site.
 *
 * Not part of CI (third-party reachability is unstable); run manually before releases or when a
 * site breaks:
 *
 * ```
 * ./gradlew :shared:source-runtime:jvmTest -Pcanary --tests "*CanarySourceTest*"
 * ```
 */
@Tag("canary")
class CanarySourceTest {

    @TempDir
    lateinit var tempDir: java.nio.file.Path

    @Test
    fun everyAnimeSourceSurvivesSearchDetailResolve() {
        val dir = File("../../demo-resources/sources/anime")
        val scripts = dir.listFiles { file -> file.isFile && file.extension == "js" }
            ?.sortedBy { it.name }
            .orEmpty()
        assertTrue(scripts.isNotEmpty(), "no anime sources found in ${dir.absolutePath}")

        val failures = mutableListOf<String>()
        for (script in scripts) {
            val name = script.nameWithoutExtension
            val started = System.nanoTime()
            try {
                runBlocking {
                    withTimeout(120_000) {
                        runCanary(name, script)
                    }
                }
                val seconds = (System.nanoTime() - started) / 1_000_000_000
                println("CANARY PASS $name (${seconds}s)")
            } catch (error: Throwable) {
                val message = error.message ?: error::class.simpleName
                println("CANARY FAIL $name: $message")
                failures += "$name: $message"
            }
        }
        assertTrue(failures.isEmpty(), "canary failures:\n${failures.joinToString("\n")}")
    }

    private suspend fun runCanary(name: String, script: File) {
        val dataDir = Path(tempDir.resolve(name).toString())
        val source = loadJsAnimeSource(script = script.readText(), dataDir = dataDir)
        try {
            val search = source.search("海贼王", page = 1)
            val first = search.items.firstOrNull()
                ?: error("search returned no results for ${source.descriptor.name}")
            val details = source.details(first.id)
            assertTrue(details.episodes.isNotEmpty(), "${first.title} has no episodes")
            val episode = details.episodes.first()
            val resolved = source.resolve(episode.url)
            assertTrue(resolved.streams.isNotEmpty(), "${first.title}/${episode.name} resolved no streams")
            assertTrue(resolved.streams.first().url.startsWith("http"), "stream url is not absolute")
        } finally {
            source.close()
        }
    }
}
