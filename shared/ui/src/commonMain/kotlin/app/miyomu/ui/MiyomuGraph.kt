package app.miyomu.ui

import app.miyomu.data.MiyomuData
import app.miyomu.danmaku.DanmakuCredentials
import app.miyomu.danmaku.DandanplayRepository
import app.miyomu.download.DownloadQueue
import app.miyomu.download.FileDownloadStore
import app.miyomu.ui.library.LibraryUpdater
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.repo.SourceInstallManager
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
    val data: MiyomuData,
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
) {
    val libraryPreferences: LibraryPreferences = LibraryPreferences(preferenceStore)

    val readerPreferences: ReaderPreferences = ReaderPreferences(preferenceStore)

    val danmakuKeys: DanmakuPrefKeys = DanmakuPrefKeys

    val libraryUpdater: LibraryUpdater by lazy { LibraryUpdater(this, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)) }

    /** Danmaku repo; credentials are re-read from prefs before each load (settings may change). */
    val danmaku: DandanplayRepository by lazy {
        DandanplayRepository(
            httpClient = io.ktor.client.HttpClient(),
            initialCredentials = readDanmakuCredentials(),
        )
    }

    fun readDanmakuCredentials(): DanmakuCredentials = DanmakuCredentials(
        appId = preferenceStore.getString(DanmakuPrefKeys.APP_ID).get(),
        appSecret = preferenceStore.getString(DanmakuPrefKeys.SECRET).get(),
    )
}

object DanmakuPrefKeys {
    const val APP_ID = "danmaku_app_id"
    const val SECRET = "danmaku_app_secret"
    const val ENABLED = "danmaku_enabled"
}
