package app.miyomu.reader.novel

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.model.MediaType
import app.miyomu.novel.LocalNovelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NovelReaderControllerTest {

    @TempDir
    lateinit var tempDir: Path

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    private fun writeBook(name: String, vararg lines: String): LocalBook {
        val file = tempDir.resolve(name)
        Files.write(file, lines.joinToString("\n").toByteArray())
        return LocalBook(
            id = file.toString(),
            title = name.substringBeforeLast('.'),
            format = LocalBookFormat.TXT,
            mediaType = MediaType.NOVEL,
            location = file.toString(),
        )
    }

    private fun NovelReaderController.awaitLoaded(): NovelReaderController.State {
        runBlocking {
            withTimeout(5_000) {
                while (state.value.loading) {
                    kotlinx.coroutines.delay(10)
                }
            }
        }
        return state.value
    }

    @Test
    fun `opens a chapter and navigates between chapters`() {
        val book = writeBook("book.txt", "第一章 甲", "甲的正文", "第二章 乙", "乙的正文")
        val controller = NovelReaderController(LocalNovelRepository(), scope)

        controller.open(book, chapterIndex = 0, charOffset = 0)
        val state = controller.awaitLoaded()

        assertEquals(2, state.chapterCount)
        assertEquals(0, state.chapterIndex)
        assertEquals("第一章 甲", state.chapterTitle)
        assertEquals("甲的正文", state.text)
        assertTrue(state.hasNextChapter)
        assertFalse(state.hasPreviousChapter)

        controller.nextChapter()
        val second = controller.awaitLoaded()
        assertEquals(1, second.chapterIndex)
        assertEquals("第二章 乙", second.chapterTitle)
        assertEquals("乙的正文", second.text)
        assertTrue(second.hasPreviousChapter)
        assertFalse(second.hasNextChapter)

        controller.previousChapter()
        assertEquals(0, controller.awaitLoaded().chapterIndex)
    }

    @Test
    fun `restores pending offset only for the requested chapter`() {
        val book = writeBook("offset.txt", "第一章 甲", "甲的正文内容", "第二章 乙", "乙的正文内容")
        val controller = NovelReaderController(LocalNovelRepository(), scope)

        controller.open(book, chapterIndex = 1, charOffset = 2)
        val state = controller.awaitLoaded()

        assertEquals(1, state.chapterIndex)
        assertEquals(2, state.pendingOffset)

        controller.previousChapter()
        val first = controller.awaitLoaded()
        assertEquals(0, first.pendingOffset, "navigation must not inherit the previous offset")
    }

    @Test
    fun `out-of-range chapter index is clamped`() {
        val book = writeBook("clamp.txt", "第一章 甲", "甲的正文")
        val controller = NovelReaderController(LocalNovelRepository(), scope)

        controller.open(book, chapterIndex = 99, charOffset = 0)
        val state = controller.awaitLoaded()

        assertEquals(0, state.chapterIndex)
        assertEquals(1, state.chapterCount)
        assertNull(state.error)
    }

    @Test
    fun `missing file surfaces an error`() {
        val missing = tempDir.resolve("missing.txt")
        val book = LocalBook(
            id = missing.toString(),
            title = "missing",
            format = LocalBookFormat.TXT,
            mediaType = MediaType.NOVEL,
            location = missing.toString(),
        )
        val controller = NovelReaderController(LocalNovelRepository(), scope)

        controller.open(book, chapterIndex = 0, charOffset = 0)
        runBlocking {
            withTimeout(5_000) {
                while (controller.state.value.loading) kotlinx.coroutines.delay(10)
            }
        }

        assertTrue(controller.state.value.error != null)
    }
}
