package app.miyomu.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.source.repo.AvailableSource
import app.miyomu.source.repo.InstalledSource
import app.miyomu.source.repo.SourceCatalog
import app.miyomu.source.repo.SourceInstallManager
import app.miyomu.source.repo.SourcePackage
import app.miyomu.ui.sources.SourceRepositoryContent
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File

@OptIn(ExperimentalTestApi::class)
class SourceRepositoryScreenTest {

    @TempDir
    lateinit var root: File

    private val repositoryUrl = "https://repo.test/index.json"
    private val packageInfo = SourcePackage(
        id = "demo",
        name = "Demo source",
        version = "1.1",
        url = "demo.js",
        lang = "en",
    )

    private fun fakeManager(): FakeSourceInstallManager = FakeSourceInstallManager(
        installed = listOf(
            InstalledSource(
                id = "demo",
                name = "Demo source",
                version = "1.0",
                lang = "en",
                scriptPath = File(root, "sources/demo/source.js").absolutePath,
                repositoryUrl = repositoryUrl,
                installedAt = 1_000L,
                updatedAt = 1_000L,
            ),
        ),
        repositories = listOf(repositoryUrl),
        catalog = SourceCatalog(
            available = listOf(AvailableSource(packageInfo, repositoryUrl, installedVersion = "1.0")),
        ),
    )

    @Test
    fun moreTabOpensTheSourceRepository() = runComposeUiTest {
        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "navigation.db").absolutePath),
        )

        try {
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 1_000L },
                    sourceInstalls = fakeManager(),
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("更多").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("更多").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("源仓库").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("源仓库").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("添加仓库").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            data.close()
        }
    }

    @Test
    fun showsInstalledUpdatesAndRepositories() = runComposeUiTest {
        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "sources.db").absolutePath),
        )
        val manager = fakeManager()

        try {
            setContent {
                SourceRepositoryContent(
                    graph = MiyomuGraph(
                        library = LocalLibraryRepository(),
                        comics = FileLocalComicRepository(),
                        data = data,
                        root = FileLocalFileRef(root),
                        deviceId = "test",
                        nowMillis = { 1_000L },
                        preferenceStore = InMemoryPreferenceStore(),
                        sourceInstalls = manager,
                    ),
                    onBack = {},
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Demo source").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("更新到 1.1").fetchSemanticsNodes().isNotEmpty()
            }
            listOf("已安装（1）", "仓库", "添加仓库", repositoryUrl).forEach { text ->
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
                }
            }
        } finally {
            data.close()
        }
    }
}

private class FakeSourceInstallManager(
    installed: List<InstalledSource>,
    repositories: List<String>,
    private val catalog: SourceCatalog,
) : SourceInstallManager {

    override val installed = MutableStateFlow(installed)
    override val repositories = MutableStateFlow(repositories)

    override suspend fun ensureLoaded() = Unit

    override suspend fun addRepository(url: String) = Unit

    override suspend fun removeRepository(url: String) = Unit

    override suspend fun fetchCatalog(): SourceCatalog = catalog

    override suspend fun getById(id: String): InstalledSource? =
        installed.value.firstOrNull { it.id == id }

    override suspend fun install(source: AvailableSource): InstalledSource =
        error("not used in this test")

    override suspend fun uninstall(id: String) = Unit
}
