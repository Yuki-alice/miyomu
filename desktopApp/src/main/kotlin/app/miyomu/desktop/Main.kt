package app.miyomu.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.domain.preferences.JsonPreferenceStore
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.download.FileDownloadQueue
import app.miyomu.download.FileDownloadStore
import app.miyomu.source.manager.FileSourceRuntimeManager
import app.miyomu.source.repo.FileSourceInstallManager
import app.miyomu.ui.MiyomuApp
import java.awt.Desktop
import java.io.File
import kotlinx.io.files.Path

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Miyomu",
        state = rememberWindowState(width = 1200.dp, height = 820.dp),
    ) {
        val home = remember {
            File(System.getProperty("user.home"), ".miyomu").apply { mkdirs() }
        }
        val libraryRoot = remember {
            File(home, "library").apply { mkdirs() }
        }
        val data = remember {
            createMiyomuData(
                JvmMiyomuDatabaseFactory(File(home, "miyomu.db").absolutePath),
            )
        }
        val root = remember { FileLocalFileRef(libraryRoot) }
        val comics = remember { FileLocalComicRepository() }
        val library = remember { LocalLibraryRepository() }
        val preferences = remember { JsonPreferenceStore(File(home, "preferences.json")) }
        val sourceInstalls = remember {
            FileSourceInstallManager(Path(File(home, "sources").absolutePath))
        }
        val scope = rememberCoroutineScope()
        val sourceRuntime = remember { FileSourceRuntimeManager(sourceInstalls, scope) }
        val downloadStore = remember { FileDownloadStore(File(home, "downloads")) }
        val downloadQueue = remember { FileDownloadQueue(sourceRuntime, downloadStore, scope) }
        DisposableEffect(sourceRuntime) {
            onDispose {
                downloadQueue.close()
                sourceRuntime.close()
            }
        }

        DisposableEffect(data) {
            onDispose { data.close() }
        }

        MiyomuApp(
            library = library,
            comics = comics,
            data = data,
            root = root,
            deviceId = "desktop",
            nowMillis = System::currentTimeMillis,
            preferenceStore = preferences,
            sourceInstalls = sourceInstalls,
            sourceRuntime = sourceRuntime,
            downloadQueue = downloadQueue,
            downloadStore = downloadStore,
            backupFile = java.io.File(home, "backup.json"),
            onOpenLibraryFolder = {
                runCatching { Desktop.getDesktop().open(libraryRoot) }
            },
        )
    }
}
