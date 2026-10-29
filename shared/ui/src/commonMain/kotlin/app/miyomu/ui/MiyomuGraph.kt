package app.miyomu.ui

import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.reader.novel.NovelBookmarkStore
import app.miyomu.source.novel.LegadoSourceStore
import app.miyomu.reader.novel.NovelReaderPreferences
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences

/**
 * Constructor-injection replacement for Aniyomi's Injekt graph: one instance wired by the
 * platform entry points, consumed by tabs and screens.
 *
 * [preferenceStore] is created by the platform (a JSON file next to the database) and shared by
 * every preference group, mirroring how Aniyomi hands `AndroidPreferenceStore` to Injekt.
 */
class MiyomuGraph(
    val library: LocalLibraryRepository,
    val comics: LocalComicRepository,
    val novels: LocalNovelRepository,
    val novelLibrary: NovelLibraryRepository,
    val data: MiyomuData,
    val root: LocalFileRef,
    val deviceId: String,
    val nowMillis: () -> Long,
    val preferenceStore: PreferenceStore,
    val onOpenLibraryFolder: (() -> Unit)? = null,
    /** Fetches an online novel URL as text; null disables the legado online flow. */
    val novelFetcher: (suspend (String) -> String)? = null,
) {
    val libraryPreferences: LibraryPreferences = LibraryPreferences(preferenceStore)

    val readerPreferences: ReaderPreferences = ReaderPreferences(preferenceStore)

    val novelReaderPreferences: NovelReaderPreferences = NovelReaderPreferences(preferenceStore)

    val novelBookmarks: NovelBookmarkStore = NovelBookmarkStore(preferenceStore)

    val novelSourceStore: LegadoSourceStore = LegadoSourceStore(preferenceStore)
}
