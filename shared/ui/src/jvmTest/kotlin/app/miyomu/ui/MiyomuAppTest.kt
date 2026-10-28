package app.miyomu.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.runComposeUiTest
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.model.LibraryItem
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalTestApi::class)
class MiyomuAppTest {

    @TempDir
    lateinit var root: File

    @Test
    fun libraryDetailAndReaderFlow() = runComposeUiTest {
        val book = File(root, "Sample Book").apply { mkdirs() }
        val chapter = File(book, "chapter 1").apply { mkdirs() }
        writePng(File(chapter, "1.png"), Color.BLUE)
        writePng(File(chapter, "2.png"), Color.ORANGE)

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "test.db").absolutePath),
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
                )
            }

            val bookTag = "book-${FileLocalFileRef(book).location}"
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(bookTag).fetchSemanticsNodes().isNotEmpty()
            }
            capture("library")
            onNodeWithTag(bookTag).performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("start-reading").fetchSemanticsNodes().isNotEmpty()
            }
            capture("detail")
            onNodeWithTag("start-reading").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("1 / 2").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("reader-pager").performTouchInput { swipeLeft() }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("2 / 2").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithContentDescription("阅读模式").performClick()
            onNodeWithText("纵向翻页").performClick()
            onAllNodesWithText("2 / 2").fetchSemanticsNodes().isNotEmpty()
            onNodeWithContentDescription("阅读模式").performClick()
            onNodeWithText("从右到左").performClick()
            onAllNodesWithText("2 / 2").fetchSemanticsNodes().isNotEmpty()
            capture("reader")
            onNodeWithContentDescription("下一章").assertExists()
        } finally {
            data.close()
        }
    }

    @Test
    fun continueReadingStartsAtSavedPage() = runComposeUiTest {
        val book = File(root, "Resume Book").apply { mkdirs() }
        val chapter = File(book, "chapter 1").apply { mkdirs() }
        writePng(File(chapter, "1.png"), Color.BLUE)
        writePng(File(chapter, "2.png"), Color.ORANGE)

        val data = createMiyomuData(
            JvmMiyomuDatabaseFactory(File(root, "resume.db").absolutePath),
        )
        val bookLocation = FileLocalFileRef(book).location
        val chapterLocation = FileLocalFileRef(chapter).location
        runBlocking {
            data.progress.save(
                ReadingProgress(
                    itemId = LibraryItem.idOf("local", bookLocation),
                    entryId = chapterLocation,
                    kind = ProgressKind.PAGE,
                    position = 1.0,
                    updatedAt = 1_000L,
                    deviceId = "test",
                ),
            )
        }

        try {
            setContent {
                MiyomuApp(
                    library = LocalLibraryRepository(),
                    comics = FileLocalComicRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 2_000L },
                )
            }

            val bookTag = "book-$bookLocation"
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag(bookTag).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(bookTag).performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("继续阅读").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("start-reading").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("2 / 2").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            data.close()
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.capture(name: String) {
        val directory = File("build/reports/miyomu-ui").apply { mkdirs() }
        val bitmap = onRoot().captureToImage().asSkiaBitmap()
        val data = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            ?: error("failed to encode $name")
        File(directory, "$name.png").writeBytes(data.bytes)
    }

    private fun writePng(file: File, color: Color) {
        val image = BufferedImage(24, 36, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.dispose()
        ImageIO.write(image, "png", file)
    }
}
