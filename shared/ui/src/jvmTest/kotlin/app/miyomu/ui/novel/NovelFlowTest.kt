package app.miyomu.ui.novel

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.model.LibraryItem
import app.miyomu.model.MediaType
import app.miyomu.model.ProgressKind
import app.miyomu.model.ReadingProgress
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.ui.MiyomuApp
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NovelFlowTest {

    @TempDir
    lateinit var root: File

    private fun novelFixtureText(): String = buildString {
        append("第一章 起点\n")
        repeat(200) { index ->
            append("这是第一章的第 $index 个段落,内容用来把文本撑到若干页,方便验证分页与翻页。\n")
        }
        append("第二章 转折\n")
        repeat(100) { index ->
            append("第二章的第 $index 个段落,继续撑长正文,保证章节边界可以导航。\n")
        }
    }

    private fun writeNovelFixture(): File {
        val book = File(root, "测试小说.txt")
        book.writeText(novelFixtureText(), Charsets.UTF_8)
        return book
    }

    private fun scannedBook(bookFile: File): LocalBook {
        val location = FileLocalFileRef(bookFile).location
        return LocalBook(
            id = location,
            title = bookFile.nameWithoutExtension,
            format = LocalBookFormat.TXT,
            mediaType = MediaType.NOVEL,
            location = location,
        )
    }

    @Test
    fun novelLibraryDetailReaderFlow() = runComposeUiTest {
        val bookFile = writeNovelFixture()
        val data = createMiyomuData(JvmMiyomuDatabaseFactory(File(root, "novel.db").absolutePath))

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
                )
            }

            // The novels tab is the first tab; the scanned TXT shows up in its grid.
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("测试小说").fetchSemanticsNodes().isNotEmpty()
            }
            Thread.sleep(400)
            capture("novel-library")
            onNodeWithText("测试小说").performClick()

            // Detail lists the parsed chapters; tapping one opens the reader at offset 0.
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("第一章 起点").fetchSemanticsNodes().isNotEmpty()
            }
            Thread.sleep(400)
            capture("novel-detail")
            onNodeWithText("第一章 起点").performClick()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("novel-reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("novel-page-0").fetchSemanticsNodes().isNotEmpty()
            }
            Thread.sleep(600)
            capture("novel-reader")

            // The chrome shows "1 / N"; swiping advances to page 2.
            onNodeWithTag("novel-reader-pager").performTouchInput { swipeLeft() }
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("novel-position", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .any { node ->
                        node.config.getOrNull(SemanticsProperties.Text)?.any { it.contains("2 /") } == true
                    }
            }

            // Progress was saved for the novel item (kind PAGE, positional chapter id).
            runBlocking {
                val itemId = LibraryItem.idOf("local", FileLocalFileRef(bookFile).location)
                var saved: ReadingProgress? = null
                repeat(40) {
                    saved = data.progress.latest(itemId)
                    if (saved != null) return@repeat
                    Thread.sleep(50)
                }
                assertTrue(saved != null, "expected a saved reading progress row")
                assertTrue(saved!!.kind == ProgressKind.PAGE)
            }
        } finally {
            data.close()
        }
    }

    @Test
    fun novelResumeRestoresOffset() = runComposeUiTest {
        val bookFile = writeNovelFixture()
        val book = scannedBook(bookFile)
        val data = createMiyomuData(JvmMiyomuDatabaseFactory(File(root, "resume.db").absolutePath))

        // An offset near the end of chapter 1; the reader must land past page 0.
        runBlocking {
            data.progress.save(
                ReadingProgress(
                    itemId = LibraryItem.idOf("local", book.location),
                    entryId = LocalNovelRepository.chapterId(book, 0),
                    kind = ProgressKind.PAGE,
                    position = (novelFixtureText().length - 150).toDouble(),
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
                    novels = LocalNovelRepository(),
                    novelLibrary = NovelLibraryRepository(),
                    data = data,
                    root = FileLocalFileRef(root),
                    deviceId = "test",
                    nowMillis = { 2_000L },
                )
            }

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("测试小说").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText("测试小说").performClick()

            val actionLabels = listOf("Resume", "继续阅读", "继续", "Start", "开始")
            waitUntil(timeoutMillis = 10_000) {
                actionLabels.any { label -> onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            }
            val actionLabel = actionLabels.first { label ->
                onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithText(actionLabel).performClick()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("novel-reader-pager").fetchSemanticsNodes().isNotEmpty()
            }
            // Restore put us on the page containing the saved offset, i.e. not page 0.
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("novel-position", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .any { node ->
                        node.config.getOrNull(SemanticsProperties.Text)
                            ?.any { label -> label.contains("/") && !label.startsWith("1 /") } == true
                    }
            }
        } finally {
            data.close()
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.capture(name: String) {
        val directory = File("build/reports/miyomu-ui").apply { mkdirs() }
        val bitmap = onRoot().captureToImage().asSkiaBitmap()
        val encoded = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)
            ?: error("failed to encode $name")
        File(directory, "$name.png").writeBytes(encoded.bytes)
    }
}
