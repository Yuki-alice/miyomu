package app.miyomu.application

import app.miyomu.data.MiyomuDatabaseFactory
import app.miyomu.data.MiyomuData
import app.miyomu.data.createMiyomuData
import app.miyomu.domain.preferences.JsonPreferenceStore
import app.miyomu.download.FileDownloadStore
import app.miyomu.download.DownloadQueue
import app.miyomu.download.FileDownloadQueue
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.local.LocalComicRepository
import app.miyomu.network.cookies.FileCookiesStorage
import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.source.manager.FileSourceRuntimeManager
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.repo.FileSourceInstallManager
import app.miyomu.source.repo.SourceInstallManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.io.files.Path

/**
 * The long-lived service container, owned by the platform (Android `Application`, desktop
 * `main`) instead of the composition: Activity recreation or a window relayout must neither
 * close the database nor cancel in-flight downloads.
 *
 * Implements [Closeable]; desktop closes it when the last window exits, Android lets it live
 * for the process lifetime.
 */
class MiyomuServices private constructor(
    val data: MiyomuData,
    val preferences: JsonPreferenceStore,
    val comics: LocalComicRepository,
    val library: LocalLibraryRepository,
    val novels: LocalNovelRepository,
    val novelLibrary: NovelLibraryRepository,
    val sourceInstalls: SourceInstallManager,
    val sourceRuntime: SourceRuntimeManager,
    val downloadStore: FileDownloadStore,
    val downloadQueue: DownloadQueue,
    val novelClient: HttpClient,
    val novelFetcher: suspend (String) -> String,
    /** App-level scope running source runtimes and the download queue; cancelled on close. */
    private val scope: CoroutineScope,
) : Closeable {

    override fun close() {
        downloadQueue.close()
        sourceRuntime.close()
        novelClient.close()
        scope.cancel()
        data.close()
    }

    companion object {
        /**
         * Builds every service that must outlive the UI. [dataDir] backs preferences, source
         * installs, downloads and cookies; [databaseFactory] is the platform Room factory.
         */
        fun create(dataDir: File, databaseFactory: MiyomuDatabaseFactory): MiyomuServices {
            val data = createMiyomuData(databaseFactory)
            val preferences = JsonPreferenceStore(File(dataDir, "preferences.json"))
            val comics = FileLocalComicRepository()
            val library = LocalLibraryRepository()
            val novels = LocalNovelRepository()
            val novelLibrary = NovelLibraryRepository()
            val sourceInstalls = FileSourceInstallManager(
                Path(File(dataDir, "sources").absolutePath),
            )
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val sourceRuntime = FileSourceRuntimeManager(sourceInstalls, scope)
            val downloadStore = FileDownloadStore(File(dataDir, "downloads"))
            val downloadQueue = FileDownloadQueue(sourceRuntime, downloadStore, scope)
            val novelClient = createMiyomuHttpClient(
                FileCookiesStorage(Path(File(dataDir, "cookies.json").absolutePath)),
            )
            val novelFetcher: suspend (String) -> String = { url -> novelClient.get(url).bodyAsText() }
            return MiyomuServices(
                data = data,
                preferences = preferences,
                comics = comics,
                library = library,
                novels = novels,
                novelLibrary = novelLibrary,
                sourceInstalls = sourceInstalls,
                sourceRuntime = sourceRuntime,
                downloadStore = downloadStore,
                downloadQueue = downloadQueue,
                novelClient = novelClient,
                novelFetcher = novelFetcher,
                scope = scope,
            )
        }
    }
}
