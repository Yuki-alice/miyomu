package app.miyomu.novel

import app.miyomu.local.LocalBook
import app.miyomu.local.LocalBookFormat
import app.miyomu.model.MediaType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class LocalNovelRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private fun txtBook(path: Path): LocalBook = LocalBook(
        id = path.toString(),
        title = path.fileName.toString().substringBeforeLast('.'),
        format = LocalBookFormat.TXT,
        mediaType = MediaType.NOVEL,
        location = path.toString(),
    )

    @Test
    fun `parses from disk and caches per book`() = runTest {
        val path = tempDir.resolve("cached.txt")
        Files.write(path, listOf("第一章 甲", "甲内容", "第二章 乙", "乙内容"))

        val repository = LocalNovelRepository()
        val book = txtBook(path)

        val first = repository.parse(book)
        assertEquals(2, first.chapters.size)
        assertSame(first, repository.parse(book), "second parse should hit the LRU cache")
        assertEquals("甲内容", repository.chapterText(book, 0))
        assertEquals("乙内容", repository.chapterText(book, 1))
    }

    @Test
    fun `rejects unsupported formats`() = runTest {
        val directory = tempDir.resolve("dir-book")
        Files.createDirectory(directory)

        val repository = LocalNovelRepository()
        val book = LocalBook(
            id = directory.toString(),
            title = "dir-book",
            format = LocalBookFormat.DIRECTORY,
            mediaType = MediaType.NOVEL,
            location = directory.toString(),
        )

        assertFailsWith<NovelParseException> { repository.parse(book) }
    }

    @Test
    fun `chapter ids are positional and stable`() {
        val book = txtBook(tempDir.resolve("stable.txt"))

        assertEquals(
            LocalNovelRepository.chapterId(book, 3),
            LocalNovelRepository.chapterId(book, 3),
        )
        assertEquals(
            LocalNovelRepository.chapterId(book, 3),
            LocalNovelRepository.chapterId(book.copy(lastModified = 123L), 3),
            "id must not depend on file timestamps",
        )
    }
}
