package app.miyomu.ui

import app.miyomu.contract.DataPort
import app.miyomu.danmaku.DanmakuCredentials
import app.miyomu.danmaku.DandanplayRepository
import app.miyomu.download.DownloadQueue
import app.miyomu.download.FileDownloadStore
import app.miyomu.ui.core.LibraryUpdater
import app.miyomu.ui.core.ReadingStatsRecorder
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.reader.novel.NovelBookmarkStore
import app.miyomu.reader.novel.NovelHighlightStore
import app.miyomu.reader.novel.NovelReaderPreferences
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.novel.LegadoSourceStore
import app.miyomu.source.repo.SourceInstallManager
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import kotlinx.coroutines.cancel

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
    val data: DataPort,
    val root: LocalFileRef,
    val deviceId: String,
    val nowMillis: () -> Long,
    val preferenceStore: PreferenceStore,
    val sourceInstalls: SourceInstallManager,
    val sourceRuntime: SourceRuntimeManager,
    val downloadQueue: DownloadQueue? = null,
    val downloadStore: FileDownloadStore? = null,
    val backupFile: java.io.File? = null,
    val onOpenLibraryFolder: (() -> Unit)? = null,
    val onPickLibraryDirectory: (() -> Unit)? = null,
    /** Fetches an online novel URL as text; null disables the legado online flow. */
    val novelFetcher: (suspend (String) -> String)? = null,
) : java.io.Closeable {
    val libraryPreferences: LibraryPreferences = LibraryPreferences(preferenceStore)

    val readerPreferences: ReaderPreferences = ReaderPreferences(preferenceStore)

    val novelReaderPreferences: NovelReaderPreferences = NovelReaderPreferences(preferenceStore)

    val novelBookmarks: NovelBookmarkStore = NovelBookmarkStore(preferenceStore)

    val novelHighlights: NovelHighlightStore = NovelHighlightStore(preferenceStore)

    val novelSourceStore: LegadoSourceStore = LegadoSourceStore(preferenceStore)

    /** Daily reading-time tracking (P1-8); both readers record through it. */
    val readingStats: ReadingStatsRecorder = ReadingStatsRecorder(preferenceStore)

    val danmakuKeys: DanmakuPrefKeys = DanmakuPrefKeys

    private val libraryUpdaterScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default,
    )

    val libraryUpdater: LibraryUpdater by lazy { LibraryUpdater(this, libraryUpdaterScope) }

    private var danmakuClient: io.ktor.client.HttpClient? = null

    /** Danmaku repo; credentials are re-read from prefs before each load (settings may change). */
    val danmaku: DandanplayRepository by lazy {
        val client = io.ktor.client.HttpClient()
        danmakuClient = client
        DandanplayRepository(
            httpClient = client,
            initialCredentials = readDanmakuCredentials(),
        )
    }

    fun readDanmakuCredentials(): DanmakuCredentials = DanmakuCredentials(
        appId = preferenceStore.getString(DanmakuPrefKeys.APP_ID).get(),
        appSecret = preferenceStore.getString(DanmakuPrefKeys.SECRET).get(),
    )

    /**
     * Releases the resources this graph itself created (its updater scope and the danmaku HTTP
     * client). Databases, source runtimes and download queues are owned by the platform service
     * container and must not be closed here.
     */
    override fun close() {
        libraryUpdaterScope.cancel()
        danmakuClient?.close()
    }
}

object DanmakuPrefKeys {
    const val APP_ID = "danmaku_app_id"
    const val SECRET = "danmaku_app_secret"
    const val ENABLED = "danmaku_enabled"
}
