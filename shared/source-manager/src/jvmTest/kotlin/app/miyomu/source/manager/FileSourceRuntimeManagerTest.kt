package app.miyomu.source.manager

import app.miyomu.source.repo.AvailableSource
import app.miyomu.source.repo.InstalledSource
import app.miyomu.source.repo.SourceCatalog
import app.miyomu.source.repo.SourceInstallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileSourceRuntimeManagerTest {

    @TempDir
    lateinit var root: File

    @Test
    fun `loads an installed source and serves search details and pages`() {
        val manager = managerFor(fixtureScript())

        try {
            runBlocking {
                val state = withTimeout(20_000) { manager.state.first { !it.loading && it.sources.isNotEmpty() } }
                assertEquals(listOf("fixture"), state.sources.map { it.id })
                assertEquals("Fixture Source", state.sources.single().name)
                assertTrue(state.errors.isEmpty())

                val search = manager.search("fixture", "hello")
                assertEquals(listOf("Result hello"), search.items.map { it.title })

                val details = manager.details("fixture", "c1")
                assertEquals("Fixture Comic", details.title)
                assertEquals(listOf("Chapter 1", "Chapter 2"), details.chapters.map { it.title })

                val pages = manager.pages("fixture", "c1", details.chapters.first().id)
                assertEquals(
                    listOf("https://example.test/1.jpg", "https://example.test/2.jpg"),
                    pages.map { it.url },
                )
            }
        } finally {
            manager.close()
        }
    }

    @Test
    fun `reports a source whose key does not match the package id`() {
        val script = File(root, "bad/source.js")
        script.parentFile.mkdirs()
        script.writeText(
            """
            class BadSource extends ComicSource {
                name = "Bad";
                key = "other";
                version = "1.0.0";
                url = "";
                search = { load: async () => ({ comics: [], maxPage: 1 }) };
            }
            """.trimIndent(),
        )

        val manager = managerFor(script)
        try {
            runBlocking {
                val state = withTimeout(20_000) { manager.state.first { !it.loading && it.errors.isNotEmpty() } }
                assertTrue(state.sources.isEmpty())
                assertTrue(state.errors.getValue("fixture").contains("does not match"))
            }
        } finally {
            manager.close()
        }
    }

    private fun managerFor(script: File): FileSourceRuntimeManager {
        val installs = FakeInstalls(
            listOf(
                InstalledSource(
                    id = "fixture",
                    name = "Fixture",
                    version = "1.0.0",
                    lang = "en",
                    scriptPath = script.absolutePath,
                    repositoryUrl = "https://repo.test/index.json",
                    installedAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return FileSourceRuntimeManager(installs, scope)
    }

    private fun fixtureScript(): File {
        val script = File(root, "fixture/source.js")
        script.parentFile.mkdirs()
        script.writeText(requireNotNull(javaClass.getResource("/fixture_source.js")).readText())
        return script
    }
}

private class FakeInstalls(initial: List<InstalledSource>) : SourceInstallManager {
    override val installed = MutableStateFlow(initial)
    override val repositories = MutableStateFlow<List<String>>(emptyList())

    override suspend fun ensureLoaded() = Unit

    override suspend fun addRepository(url: String) = Unit

    override suspend fun removeRepository(url: String) = Unit

    override suspend fun fetchCatalog(): SourceCatalog = SourceCatalog()

    override suspend fun getById(id: String): InstalledSource? =
        installed.value.firstOrNull { it.id == id }

    override suspend fun install(source: AvailableSource): InstalledSource = error("unused")

    override suspend fun uninstall(id: String) = Unit
}
