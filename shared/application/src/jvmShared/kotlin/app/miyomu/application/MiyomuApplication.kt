package app.miyomu.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.miyomu.data.MiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.domain.preferences.JsonPreferenceStore
import app.miyomu.download.FileDownloadQueue
import app.miyomu.download.FileDownloadStore
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.network.cookies.FileCookiesStorage
import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.source.manager.FileSourceRuntimeManager
import app.miyomu.source.repo.FileSourceInstallManager
import app.miyomu.ui.MiyomuApp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File
import kotlinx.io.files.Path

/**
 * Everything a platform entry point has to provide. The composition root below owns the rest,
 * so Android and desktop do not duplicate the graph wiring.
 */
class MiyomuAppEnvironment(
    val dataDir: File,
    val libraryRoot: LocalFileRef,
    val databaseFactory: MiyomuDatabaseFactory,
    val deviceId: String,
    val nowMillis: () -> Long,
    val onExit: () -> Unit = {},
    val onOpenLibraryFolder: (() -> Unit)? = null,
    val onPickLibraryDirectory: (() -> Unit)? = null,
)

@Composable
fun MiyomuApplication(environment: MiyomuAppEnvironment) {
    val data = remember(environment.databaseFactory) {
        createMiyomuData(environment.databaseFactory)
    }
    val preferences = remember(environment.dataDir) {
        JsonPreferenceStore(File(environment.dataDir, "preferences.json"))
    }
    val comics = remember { FileLocalComicRepository() }
    val library = remember { LocalLibraryRepository() }
    val novels = remember { LocalNovelRepository() }
    val novelLibrary = remember { NovelLibraryRepository() }
    val sourceInstalls = remember(environment.dataDir) {
        FileSourceInstallManager(Path(File(environment.dataDir, "sources").absolutePath))
    }
    val scope = rememberCoroutineScope()
    val sourceRuntime = remember(sourceInstalls, scope) {
        FileSourceRuntimeManager(sourceInstalls, scope)
    }
    val downloadStore = remember(environment.dataDir) {
        FileDownloadStore(File(environment.dataDir, "downloads"))
    }
    val downloadQueue = remember(sourceRuntime, downloadStore, scope) {
        FileDownloadQueue(sourceRuntime, downloadStore, scope)
    }
    val novelClient = remember(environment.dataDir) {
        createMiyomuHttpClient(
            FileCookiesStorage(Path(File(environment.dataDir, "cookies.json").absolutePath)),
        )
    }
    val novelFetcher: suspend (String) -> String = remember(novelClient) {
        { url -> novelClient.get(url).bodyAsText() }
    }

    DisposableEffect(sourceRuntime, downloadQueue, novelClient) {
        onDispose {
            downloadQueue.close()
            sourceRuntime.close()
            novelClient.close()
        }
    }
    DisposableEffect(data) {
        onDispose { data.close() }
    }

    MiyomuApp(
        library = library,
        comics = comics,
        novels = novels,
        novelLibrary = novelLibrary,
        data = data,
        root = environment.libraryRoot,
        deviceId = environment.deviceId,
        nowMillis = environment.nowMillis,
        preferenceStore = preferences,
        sourceInstalls = sourceInstalls,
        sourceRuntime = sourceRuntime,
        downloadQueue = downloadQueue,
        downloadStore = downloadStore,
        backupFile = File(environment.dataDir, "backup.json"),
        onOpenLibraryFolder = environment.onOpenLibraryFolder,
        onPickLibraryDirectory = environment.onPickLibraryDirectory,
        novelFetcher = novelFetcher,
        onExit = environment.onExit,
    )
}
