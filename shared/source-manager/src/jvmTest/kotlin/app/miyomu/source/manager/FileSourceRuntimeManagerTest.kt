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
    fun `loads an anime source alongside a comic source and resolves video`() {
        val comicScript = File(root, "fixture/source.js")
        comicScript.parentFile.mkdirs()
        comicScript.writeText(requireNotNull(javaClass.getResource("/fixture_source.js")).readText())
        val animeScript = File(root, "anime/source.js")
        animeScript.parentFile.mkdirs()
        animeScript.writeText(animeFixtureScript())

        val installs = FakeInstalls(
            listOf(
                InstalledSource(
                    id = "fixture",
                    name = "Fixture",
                    version = "1.0.0",
                    lang = "en",
                    scriptPath = comicScript.absolutePath,
                    repositoryUrl = "https://repo.test/index.json",
                    installedAt = 1L,
                    updatedAt = 1L,
                ),
                InstalledSource(
                    id = "anime_fixture",
                    name = "Anime Fixture",
                    version = "1.0.0",
                    lang = "zh",
                    scriptPath = animeScript.absolutePath,
                    repositoryUrl = "https://repo.test/index.json",
                    installedAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = FileSourceRuntimeManager(installs, scope)

        try {
            runBlocking {
                val state = withTimeout(20_000) { manager.state.first { !it.loading && it.sources.size == 2 } }
                assertEquals(
                    listOf("VIDEO", "COMIC"),
                    state.sources.sortedBy { it.id }.map { it.mediaType },
                )

                val search = manager.search("anime_fixture", "夜")
                assertEquals("夜行西行", search.items.single().title)

                val details = manager.details("anime_fixture", "a1")
                assertEquals(2, details.episodes.size)
                assertEquals("线路1", details.episodes.first().name)

                val resolved = manager.resolveVideo("anime_fixture", details.episodes.first().url)
                assertEquals("https://stream.test/a1.m3u8", resolved.streams.single().url)
                assertEquals("https://stream.test/", resolved.headers["Referer"])
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

    private fun animeFixtureScript(): String = """
        class AnimeFixtureSource extends AnimeSource {
            name = "Anime Fixture";
            key = "anime_fixture";
            version = "1.0.0";
            url = "";

            async search(query, page) {
                return { list: [{ id: "a1", title: "夜行西行" }], maxPage: 1 };
            }

            async detail(id) {
                return {
                    title: "Anime Fixture",
                    episodes: [
                        { key: "https://stream.test/a1", name: "线路1", url: "https://stream.test/a1", line: 0, number: 1 },
                        { key: "https://stream.test/a2", name: "线路2", url: "https://stream.test/a2", line: 1, number: 1 }
                    ]
                };
            }

            async resolve(episodeUrl) {
                return {
                    urls: [{ url: "https://stream.test/a1.m3u8", quality: "1080P", type: "hls" }],
                    headers: { Referer: "https://stream.test/" }
                };
            }
        }
    """.trimIndent()
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
