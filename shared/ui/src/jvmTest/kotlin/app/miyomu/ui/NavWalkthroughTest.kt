package app.miyomu.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.source.repo.FileSourceInstallManager
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlinx.io.files.Path
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Walks the destinations that only the shell can reach — the settings chain, the utility pages and
 * back-navigation — through the real [app.miyomu.ui.MiyomuApp] and the real
 * [app.miyomu.ui.navigation.MiyomuNavHost].
 *
 * The other flow tests cover library → detail → reader; this one covers the part of the graph that
 * had no coverage at all before the Navigation 3 migration, and it is the only place that asserts
 * `pop()` actually returns to the previous screen.
 */
@OptIn(ExperimentalTestApi::class)
class NavWalkthroughTest {

    @TempDir
    lateinit var root: File

    @Test
    fun utilityDestinationsOpenAndPopBack() = runComposeUiTest {
        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "walkthrough.db").absolutePath),
        )

        try {
            setContent { appUnderTest(data) }

            clickTab("更多")
            awaitText("源仓库")

            // Settings hub ------------------------------------------------------------------
            clickText("设置")
            awaitText("外观")
            capture("nav-settings")

            // Every settings page must open and pop back to the hub.
            openAndPop("外观", "主题模式", "nav-settings-appearance")
            openAndPop("阅读", "默认方向", "nav-settings-reader")
            openAndPop("下载", "删除全部下载", "nav-settings-download")
            openAndPop("数据与存储", "清除阅读历史", "nav-settings-data")
            openAndPop("弹幕", "AppSecret", "nav-settings-danmaku")
            openAndPop("关于 Miyomu", "Miyomu 0.1.0", "nav-settings-about")

            // Back from the hub lands on the tab shell again.
            back()
            awaitText("源仓库")
            capture("nav-back-to-more")

            // Utility pages reachable from the More tab --------------------------------------
            clickText("源仓库")
            awaitText("添加仓库")
            capture("nav-source-repository")
            back()
            awaitText("下载队列")

            clickText("备份与同步")
            awaitText("本地备份")
            capture("nav-backup")
            back()
            awaitText("下载队列")

            clickText("下载队列")
            awaitText("没有下载任务")
            capture("nav-downloads")
            back()
            awaitText("下载队列")
        } finally {
            data.close()
        }
    }

    @Test
    fun comicDetailPopsBackToTheLibrary() = runComposeUiTest {
        val book = File(root, "Sample Book").apply { mkdirs() }
        val chapter = File(book, "chapter 1").apply { mkdirs() }
        writePng(File(chapter, "1.png"), Color.BLUE)

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "detail-back.db").absolutePath),
        )

        try {
            setContent { appUnderTest(data) }

            clickTab("书架")
            awaitText("Sample Book")
            clickText("Sample Book")

            awaitText("chapter 1")
            capture("nav-detail")

            // Reader is a full-screen destination; popping it must return to the detail screen.
            clickText("chapter 1")
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            settle()
            capture("nav-reader")
            back()
            awaitText("chapter 1")
            capture("nav-reader-back-to-detail")

            // pop() must land back on the shelf, not on an empty stack or a stale destination.
            back()
            awaitText("Sample Book")
            capture("nav-detail-back-to-library")
        } finally {
            data.close()
        }
    }


    /**
     * The anime-source path carries the whole play queue inside the key, so it is the riskiest
     * destination in the graph: detail → player → back has to work without the detail screen
     * staying composed behind the player.
     */
    @Test
    fun animeSourceReachesDetailAndPlayer() = runComposeUiTest {
        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "anime.db").absolutePath),
        )
        val runtime = FakeSourceRuntimeManager(
            state = app.miyomu.source.manager.SourceRuntimeState(
                sources = listOf(
                    app.miyomu.source.manager.SourceRuntimeInfo(
                        id = "anime",
                        name = "Fixture Anime",
                        lang = "en",
                        version = "1.0.0",
                        mediaType = "VIDEO",
                    ),
                ),
            ),
            searchResults = listOf(app.miyomu.model.ComicSummary(id = "a1", title = "Fixture Show")),
            details = app.miyomu.model.ComicDetails(
                id = "a1",
                title = "Fixture Show",
                episodes = listOf(
                    app.miyomu.model.AnimeEpisode(key = "e1", name = "E01", url = "https://example.test/e1"),
                    app.miyomu.model.AnimeEpisode(key = "e2", name = "E02", url = "https://example.test/e2"),
                ),
            ),
            resolvedVideo = app.miyomu.model.ResolvedVideo(
                streams = listOf(app.miyomu.model.ResolvedStream(url = "https://example.test/e1.m3u8")),
                headers = mapOf("Referer" to "https://example.test"),
            ),
        )

        try {
            setContent { appUnderTest(data, runtime) }

            clickTab("浏览")
            clickText("Fixture Anime")
            awaitText("搜索")
            onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("show")
            onNode(androidx.compose.ui.test.hasSetTextAction()).performImeAction()

            awaitText("Fixture Show")
            clickText("Fixture Show")
            awaitText("E01")
            capture("nav-anime-detail")

            clickText("E01")
            waitUntil(timeoutMillis = 8_000) {
                onAllNodesWithText("下一集").fetchSemanticsNodes().isNotEmpty()
            }
            settle()
            capture("nav-anime-player")

            // Popping the player must return to the anime detail, not to the search results.
            back()
            awaitText("E01")
            capture("nav-anime-player-back-to-detail")
        } finally {
            data.close()
        }
    }

    @androidx.compose.runtime.Composable
    private fun appUnderTest(
        data: app.miyomu.contract.DataPort,
        sourceRuntime: FakeSourceRuntimeManager = FakeSourceRuntimeManager(),
    ) {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val downloadStore = app.miyomu.download.FileDownloadStore(File(root, "downloads"))
        val downloadQueue = app.miyomu.download.FileDownloadQueue(sourceRuntime, downloadStore, scope)

        MiyomuApp(
            library = LocalLibraryRepository(),
            comics = FileLocalComicRepository(),
            novels = LocalNovelRepository(),
            novelLibrary = NovelLibraryRepository(),
            data = data,
            root = FileLocalFileRef(root),
            deviceId = "test",
            nowMillis = { 1_000L },
            sourceInstalls = FileSourceInstallManager(Path(File(root, "sources").absolutePath)),
            sourceRuntime = sourceRuntime,
            downloadQueue = downloadQueue,
            downloadStore = downloadStore,
            backupFile = File(root, "backup.json"),
        )
    }

    /** Opens [row], asserts [anchor] is on screen, screenshots it, then pops back. */
    private fun ComposeUiTest.openAndPop(row: String, anchor: String, shot: String) {
        clickText(row)
        awaitText(anchor)
        capture(shot)
        back()
        awaitText("外观")
    }

    private fun ComposeUiTest.clickTab(label: String) {
        awaitText(label)
        onAllNodesWithText(label)[0].performClick()
        settle()
    }

    private fun ComposeUiTest.clickText(label: String) {
        awaitText(label)
        onAllNodesWithText(label)[0].performClick()
        settle()
    }

    private fun ComposeUiTest.back() {
        val labels = listOf("返回", "Back")
        val label = labels.first { label ->
            onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithContentDescription(label)[0].performClick()
        settle()
    }

    private fun ComposeUiTest.awaitText(text: String) {
        try {
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            // Keep the tree next to the screenshots so a failure is diagnosable from CI artifacts.
            File("build/reports/miyomu-ui").mkdirs()
            File("build/reports/miyomu-ui/nav-failure.txt").writeText(
                "missing text: $text\n\n" + onRoot().printToString(maxDepth = 20),
            )
            throw e
        }
    }

    /**
     * Navigation 3 animates between entries, so the outgoing screen is still in the tree for a
     * moment. Waiting for idle plus a short settle keeps the next click on the intended node.
     */
    private fun ComposeUiTest.settle() {
        waitForIdle()
        Thread.sleep(350)
        waitForIdle()
    }

    private fun ComposeUiTest.capture(name: String) {
        val directory = File("build/reports/miyomu-ui").apply { mkdirs() }
        val bitmap = onRoot().captureToImage().asSkiaBitmap()
        val bytes = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            ?: error("failed to encode $name")
        File(directory, "$name.png").writeBytes(bytes.bytes)
    }

    private fun writePng(file: File, color: Color) {
        val image = BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, 40, 60)
        graphics.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        file.writeBytes(out.toByteArray())
    }
}
