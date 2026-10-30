package app.miyomu.ui.novel

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.repo.FileSourceInstallManager
import app.miyomu.ui.FakeSourceRuntimeManager
import app.miyomu.ui.MiyomuApp
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.library.installLocalCoverLoader
import app.miyomu.ui.MiyomuNavHost
import app.miyomu.ui.navigation.NovelExploreKey
import kotlinx.io.files.Path
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File

@OptIn(ExperimentalTestApi::class)
class NovelExploreFlowTest {

    @TempDir
    lateinit var root: File

    private val sourceJson = """
        {
          "bookSourceUrl": "https://novel.example",
          "bookSourceName": "发现书源",
          "exploreUrl": "玄幻::/list/xh?p={{page}}",
          "ruleExplore": {
            "bookList": "div.book-item",
            "name": "h3 a@text",
            "author": ".author@text",
            "bookUrl": "h3 a@href",
            "coverUrl": "img@src"
          },
          "ruleToc": {
            "chapterList": "ul.chapters li",
            "chapterName": "a@text",
            "chapterUrl": "a@href"
          }
        }
    """.trimIndent()

    private val explorePage = """
        <html><body>
          <div class="book-item">
            <h3><a href="/book/1">斗破苍穹</a></h3>
            <span class="author">天蚕土豆</span>
          </div>
        </body></html>
    """.trimIndent()

    private val tocPage = """
        <html><body>
          <ul class="chapters">
            <li><a href="/book/1/c1">第一章 起点</a></li>
          </ul>
        </body></html>
    """.trimIndent()

    @Test
    fun browseDiscoverUsesTheRootNavigator() = runComposeUiTest {
        val data = createMiyomuData(JvmMiyomuDatabaseFactory(File(root, "explore-app.db").absolutePath))

        try {
            val source = LegadoBookSource.fromJson(sourceJson)
            val savedSources = Json.encodeToString(
                ListSerializer(LegadoBookSource.serializer()),
                listOf(source),
            )
            val preferences = InMemoryPreferenceStore(
                sequenceOf(
                    InMemoryPreferenceStore.InMemoryPreference("legado_sources", savedSources, "[]"),
                ),
            )
            installLocalCoverLoader { null }
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    novels = LocalNovelRepository(),
                    novelLibrary = NovelLibraryRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 1_000L },
                    preferenceStore = preferences,
                    sourceInstalls = FileSourceInstallManager(Path(File(root, "sources").absolutePath)),
                    sourceRuntime = FakeSourceRuntimeManager(),
                    novelFetcher = { url ->
                        when {
                            url.startsWith("https://novel.example/list/") -> explorePage
                            url.startsWith("https://novel.example/book/") -> tocPage
                            else -> error("unexpected url: $url")
                        }
                    },
                )
            }

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("浏览").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("浏览").performClick()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("发现书源").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("novel-explore-open").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("玄幻").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("玄幻").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("斗破苍穹").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("斗破苍穹").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("novel-online-chapters").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            data.close()
        }
    }

    @Test
    fun discoverCategoryOpensBookDetail() = runComposeUiTest {
        val data = createMiyomuData(JvmMiyomuDatabaseFactory(File(root, "explore.db").absolutePath))

        try {
            val graph = MiyomuGraph(
                library = LocalLibraryRepository(),
                comics = FileLocalComicRepository(),
                novels = LocalNovelRepository(),
                novelLibrary = NovelLibraryRepository(),
                data = data,
                root = FileLocalFileRef(root),
                deviceId = "test",
                nowMillis = { 1_000L },
                preferenceStore = InMemoryPreferenceStore(),
                sourceInstalls = FileSourceInstallManager(Path(File(root, "sources").absolutePath)),
                sourceRuntime = FakeSourceRuntimeManager(),
                novelFetcher = { url ->
                    when {
                        url.startsWith("https://novel.example/list/") -> explorePage
                        url.startsWith("https://novel.example/book/") -> tocPage
                        else -> error("unexpected url: $url")
                    }
                },
            )
            val source = LegadoBookSource.fromJson(sourceJson)
            graph.novelSourceStore.addSource(source)

            installLocalCoverLoader { null }
            setContent {
                MiyomuNavHost(graph, start = NovelExploreKey(source.bookSourceUrl))
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("玄幻").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("玄幻").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("斗破苍穹").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("斗破苍穹").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("novel-online-chapters").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            data.close()
        }
    }
}
