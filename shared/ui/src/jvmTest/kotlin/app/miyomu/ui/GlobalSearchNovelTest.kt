package app.miyomu.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
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
import app.miyomu.ui.library.installLocalCoverLoader
import app.miyomu.ui.sources.GlobalSearchRoute
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.io.files.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.io.File
import kotlin.test.assertTrue

/**
 * Global search used to fan out over the installed JS sources only, so imported legado novel
 * sources were invisible to it even though the same query field is the app's cross-medium
 * entry point.
 */
@OptIn(ExperimentalTestApi::class)
class GlobalSearchNovelTest {

    @TempDir
    lateinit var root: File

    private val sourceJson = """
        {
          "bookSourceUrl": "https://novel.example",
          "bookSourceName": "示例书源",
          "searchUrl": "https://novel.example/search?q={{key}}&p={{page}}",
          "ruleSearch": {
            "bookList": "div.book-item",
            "name": "h3 a@text",
            "author": ".author@text",
            "bookUrl": "h3 a@href",
            "coverUrl": "img@src"
          }
        }
    """.trimIndent()

    private val searchPage = """
        <html><body>
          <div class="book-item">
            <h3><a href="/book/1">斗破苍穹</a></h3>
            <span class="author">天蚕土豆</span>
          </div>
          <div class="book-item">
            <h3><a href="/book/2">凡人修仙传</a></h3>
            <span class="author">忘语</span>
          </div>
        </body></html>
    """.trimIndent()

    @Test
    fun globalSearchIncludesNovelSources() = runComposeUiTest {
        val data = createMiyomuData(JvmMiyomuDatabaseFactory(File(root, "global.db").absolutePath))

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
                novelFetcher = { searchPage },
            )
            graph.novelSourceStore.addSource(LegadoBookSource.fromJson(sourceJson))

            // This test renders the route directly instead of going through MiyomuApp, so it
            // has to do the cover-loader install itself: coil's singleton must be set up
            // before the first AsyncImage, and a `get()` without it poisons the whole JVM
            // for every later test in this task.
            installLocalCoverLoader { null }

            setContent { Navigator(GlobalSearchRoute(graph)) }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("全局搜索").fetchSemanticsNodes().isNotEmpty()
            }

            onNode(hasSetTextAction()).performTextInput("斗破")
            onNode(hasSetTextAction()).performImeAction()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("斗破苍穹").fetchSemanticsNodes().isNotEmpty()
            }
            // The section is labelled so an aggregated list stays readable across media.
            assertTrue(onAllNodesWithText("示例书源").fetchSemanticsNodes().isNotEmpty())
        } finally {
            data.close()
        }
    }
}
