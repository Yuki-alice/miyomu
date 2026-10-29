package app.miyomu.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithContentDescription
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
import app.miyomu.source.repo.FileSourceInstallManager
import app.miyomu.model.LibraryItem
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import kotlinx.io.files.Path
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
                    sourceInstalls = FileSourceInstallManager(Path(File(root, "sources").absolutePath)),
                    sourceRuntime = FakeSourceRuntimeManager(),
                )
            }

            // The ported library grid renders the title overlay instead of a per-item test tag.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Sample Book").fetchSemanticsNodes().isNotEmpty()
            }
            // Covers decode off the composition (coil), so give the loader a moment before the
            // evidence screenshot is taken.
            Thread.sleep(600)
            capture("library")
            onNodeWithText("Sample Book").performClick()

            // The ported detail screen lists chapters; clicking one opens the reader at page 0.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("chapter 1").fetchSemanticsNodes().isNotEmpty()
            }
            Thread.sleep(600)
            capture("detail")
            onNodeWithText("chapter 1").performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-page-0").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("reader-pager").performTouchInput { swipeLeft() }
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-page-1").fetchSemanticsNodes().isNotEmpty()
            }
            capture("reader")
            // Chapter navigation lives in the ported bottom bar.
            listOf("下一章", "Next chapter").any { label ->
                onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
            }.let { assertTrue(it, "expected a next-chapter action in the reader chrome") }
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
                    sourceInstalls = FileSourceInstallManager(Path(File(root, "sources").absolutePath)),
                    sourceRuntime = FakeSourceRuntimeManager(),
                )
            }

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Resume Book").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("Resume Book").performClick()
            // The ported detail screen puts the primary action on an extended FAB. Its label is
            // "Start" until a chapter is marked read (a single-chapter book never is), and it
            // follows the resolved locale, so accept either language.
            val actionLabels = listOf("Resume", "继续阅读", "继续", "Start", "开始")
            waitUntil(timeoutMillis = 5_000) {
                actionLabels.any { label -> onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            }
            val actionLabel = actionLabels.first { label ->
                onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(actionLabel).performClick()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            // Saved position was page index 1, so the reader must open on the second page.
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithTag("reader-page-1").fetchSemanticsNodes().isNotEmpty()
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
