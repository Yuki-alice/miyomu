package app.miyomu.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import app.miyomu.model.ComicDetails
import app.miyomu.model.ComicSummary
import app.miyomu.model.PageRef
import app.miyomu.source.manager.SourceRuntimeInfo
import app.miyomu.source.manager.SourceRuntimeState
import app.miyomu.source.repo.FileSourceInstallManager
import kotlinx.io.files.Path
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class SourceBrowseFlowTest {

    @TempDir
    lateinit var root: File

    @Test
    fun browseSearchDetailAndReadAnInstalledSource() = runComposeUiTest {
        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "sources-flow.db").absolutePath),
        )
        val runtime = FakeSourceRuntimeManager(
            state = SourceRuntimeState(
                sources = listOf(
                    SourceRuntimeInfo(
                        id = "fixture",
                        name = "Fixture Source",
                        lang = "en",
                        version = "1.0.0",
                        mediaType = "COMIC",
                    ),
                ),
            ),
            searchResults = listOf(
                ComicSummary(id = "c1", title = "Fixture Comic", author = "Fixture Author"),
            ),
            details = ComicDetails(
                id = "c1",
                title = "Fixture Comic",
                description = "Fixture description",
                chapters = listOf(
                    app.miyomu.model.Chapter(id = "ch1", title = "Chapter 1"),
                    app.miyomu.model.Chapter(id = "ch2", title = "Chapter 2"),
                ),
            ),
            pages = listOf(PageRef(url = "https://example.test/1.png")),
            imageBytes = pngBytes(),
        )

        try {
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
                    sourceInstalls = FileSourceInstallManager(Path(File(root, "sources").absolutePath)),
                    sourceRuntime = runtime,
                )
            }

            onNode(hasText("浏览") and hasClickAction()).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Fixture Source").fetchSemanticsNodes().isNotEmpty()
            }
            capture("c2-browse")

            onNode(hasText("Fixture Source") and hasClickAction()).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("搜索").fetchSemanticsNodes().isNotEmpty()
            }
            onNode(hasSetTextAction()).performTextInput("hello")
            onNode(hasSetTextAction()).performImeAction()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Fixture Comic").fetchSemanticsNodes().isNotEmpty()
            }
            capture("c2-search")

            onNode(hasText("Fixture Comic") and hasClickAction()).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Chapter 1").fetchSemanticsNodes().isNotEmpty()
            }
            capture("c2-detail")

            onNode(hasText("Chapter 1") and hasClickAction()).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-page-0").fetchSemanticsNodes().isNotEmpty()
            }
            capture("c2-reader")
        } finally {
            data.close()
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.capture(name: String) {
        val directory = File("build/reports/miyomu-ui").apply { mkdirs() }
        val bitmap = onRoot().captureToImage().asSkiaBitmap()
        val bytes = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            ?: error("failed to encode $name")
        File(directory, "$name.png").writeBytes(bytes.bytes)
    }

    private fun pngBytes(): ByteArray {
        val image = BufferedImage(120, 180, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color(0x33, 0x66, 0x99)
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.dispose()
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }
}
