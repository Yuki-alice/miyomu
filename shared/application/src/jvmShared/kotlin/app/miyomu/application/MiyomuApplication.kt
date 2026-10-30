package app.miyomu.application

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalFileRef
import app.miyomu.model.MediaType
import app.miyomu.ui.MiyomuApp
import app.miyomu.ui.navigation.MangaDetailKey
import app.miyomu.ui.navigation.MiyomuKey
import app.miyomu.ui.navigation.NovelDetailKey
import java.io.File

/**
 * Everything a platform entry point has to provide. Long-lived services come from
 * [MiyomuServices] (owned by the platform so they survive Activity recreation); the
 * environment carries only the UI-facing bits: the library root, directory hooks and exit.
 */
class MiyomuAppEnvironment(
    val dataDir: File,
    val libraryRoot: LocalFileRef,
    val deviceId: String,
    val nowMillis: () -> Long,
    val onExit: () -> Unit = {},
    val onOpenLibraryFolder: (() -> Unit)? = null,
    val onPickLibraryDirectory: (() -> Unit)? = null,
    /**
     * P1-6 系统集成: a book opened through the platform (Android ACTION_VIEW, desktop file
     * association/CLI). The platform has already copied the file into the library root, so the
     * first scan shelves it and the shell can start straight on its detail page.
     */
    val startBookLocation: String? = null,
    val startBookMediaType: MediaType? = null,
)

@Composable
fun MiyomuApplication(
    environment: MiyomuAppEnvironment,
    services: MiyomuServices,
) {
    val startKey: MiyomuKey? = environment.startBookLocation?.let { location ->
        when (environment.startBookMediaType) {
            MediaType.NOVEL -> NovelDetailKey(location)
            else -> MangaDetailKey(location)
        }
    }
    MiyomuApp(
        library = services.library,
        comics = services.comics,
        novels = services.novels,
        novelLibrary = services.novelLibrary,
        data = services.data,
        root = environment.libraryRoot,
        deviceId = environment.deviceId,
        nowMillis = environment.nowMillis,
        preferenceStore = services.preferences,
        sourceInstalls = services.sourceInstalls,
        sourceRuntime = services.sourceRuntime,
        downloadQueue = services.downloadQueue,
        downloadStore = services.downloadStore,
        backupFile = File(environment.dataDir, "backup.json"),
        onOpenLibraryFolder = environment.onOpenLibraryFolder,
        onPickLibraryDirectory = environment.onPickLibraryDirectory,
        novelFetcher = services.novelFetcher,
        startKey = startKey,
        onExit = environment.onExit,
    )
}
