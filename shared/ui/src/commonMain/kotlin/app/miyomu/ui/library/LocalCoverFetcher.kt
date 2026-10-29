package app.miyomu.ui.library

import app.miyomu.local.LocalBook
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.map.Mapper
import coil3.request.Options
import okio.Buffer
import okio.FileSystem
import tachiyomi.domain.entries.manga.model.Manga
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.entries.manga.model.asMangaCover

/**
 * Cover loader for local books.
 *
 * The ported Aniyomi grids always build a [MangaCover] from the domain `Manga` and hand it to
 * coil, so a local book arrives with `sourceId == LOCAL_SOURCE_ID` and `url == null` (there is no
 * remote thumbnail). Claiming those requests here keeps the ported UI untouched: the bytes come
 * from [app.miyomu.local.LocalComicRepository.cover] instead of the network.
 */
object LocalCoverLoader {
    /** Resolves a synthetic manga id back to its scanned book and returns the cover bytes. */
    @Volatile
    internal var load: (suspend (mangaId: Long) -> ByteArray?)? = null
}

class LocalMangaCoverFetcher(
    private val mangaId: Long,
    private val load: suspend (Long) -> ByteArray?,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bytes = load(mangaId) ?: return null
        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<MangaCover> {
        override fun create(data: MangaCover, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.sourceId != LOCAL_SOURCE_ID) return null
            val load = LocalCoverLoader.load ?: return null
            return LocalMangaCoverFetcher(data.mangaId, load)
        }
    }
}

/**
 * Registers the local cover fetcher with coil's singleton image loader. Must run before the first
 * cover is requested; [app.miyomu.ui.MiyomuApp] calls it once per process.
 */
fun installLocalCoverLoader(load: suspend (mangaId: Long) -> ByteArray?) {
    LocalCoverLoader.load = load
    coil3.SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components {
                // The detail header hands coil a `Manga` directly (Aniyomi maps it with a
                // MangaMapper in the app module, which is not ported), so the same mapping has
                // to exist here for the header cover to reach the fetcher below.
                add(MangaCoverMapper())
                add(LocalMangaCoverFetcher.Factory())
            }
            .build()
    }
}

/** `Manga` -> `MangaCover`, mirroring Aniyomi's app-level MangaMapper. */
class MangaCoverMapper : Mapper<Manga, MangaCover> {
    override fun map(data: Manga, options: Options): MangaCover = data.asMangaCover()
}

/** Convenience overload for callers that resolve covers from a book list. */
fun localCoverResolver(books: () -> List<LocalBook>, cover: suspend (LocalBook) -> ByteArray?): suspend (Long) -> ByteArray? =
    { mangaId -> books().firstOrNull { it.mangaId() == mangaId }?.let { cover(it) } }
