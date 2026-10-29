package app.miyomu.ui.library

import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalBookFormat
import app.miyomu.model.MediaType
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.domain.entries.manga.model.MangaCover
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Verifies the bridge between the ported library grid (which hands a `MangaCover` to coil) and
 * Miyomu's local file repository.
 */
class LocalCoverTest {

    @TempDir
    lateinit var root: File

    private fun sampleBook(): File {
        val book = File(root, "Cover Book").apply { mkdirs() }
        val chapter = File(book, "chapter 1").apply { mkdirs() }
        val image = BufferedImage(48, 72, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            color = Color.RED
            fillRect(0, 0, image.width, image.height)
            dispose()
        }
        ImageIO.write(image, "png", File(chapter, "1.png"))
        return book
    }

    private fun bookOf(directory: File) = app.miyomu.local.LocalBook(
        id = FileLocalFileRef(directory).location,
        title = "Cover Book",
        format = LocalBookFormat.DIRECTORY,
        mediaType = MediaType.COMIC,
        location = FileLocalFileRef(directory).location,
        lastModified = directory.lastModified(),
    )

    private fun coverOf(book: app.miyomu.local.LocalBook) = MangaCover(
        mangaId = book.mangaId(),
        sourceId = LOCAL_SOURCE_ID,
        isMangaFavorite = true,
        url = null,
        lastModified = book.lastModified,
    )

    @Test
    fun fetcherReturnsCoverBytes() = runBlocking {
        val repository = FileLocalComicRepository()
        val book = bookOf(sampleBook())

        val result = LocalMangaCoverFetcher(book.mangaId()) { id ->
            if (id == book.mangaId()) repository.cover(book) else null
        }.fetch()

        assertNotNull(result, "fetcher should produce a result for a book with pages")
        val bytes = (result as coil3.fetch.SourceFetchResult).source.source()
            .use { it.readByteArray() }
        assertTrue(bytes.isNotEmpty(), "cover bytes should not be empty")
    }

    @Test
    fun factoryIgnoresRemoteCovers() {
        LocalCoverLoader.load = { null }
        val factory = LocalMangaCoverFetcher.Factory()
        val remote = MangaCover(
            mangaId = 1L,
            sourceId = 42L,
            isMangaFavorite = true,
            url = "https://example.invalid/cover.png",
            lastModified = 0L,
        )

        val fetcher = factory.create(remote, coil3.request.Options(PlatformContext.INSTANCE), loader())

        assertNull(fetcher, "remote covers must fall through to the network fetchers")
    }

    @Test
    fun coilDecodesLocalCover() = runBlocking {
        val repository = FileLocalComicRepository()
        val book = bookOf(sampleBook())
        LocalCoverLoader.load = { id -> if (id == book.mangaId()) repository.cover(book) else null }

        val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(LocalMangaCoverFetcher.Factory()) }
            .build()

        val request = ImageRequest.Builder(PlatformContext.INSTANCE)
            .data(coverOf(book))
            .build()

        val result = imageLoader.execute(request)
        assertTrue(result is SuccessResult, "coil should decode the local cover, got $result")
    }

    private fun loader() = ImageLoader.Builder(PlatformContext.INSTANCE).build()
}
