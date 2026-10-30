package app.miyomu.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.miyomu.data.AndroidMiyomuDatabaseFactory
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
import java.io.File
import kotlinx.io.files.Path

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current.applicationContext
            val data = remember {
                createMiyomuData(AndroidMiyomuDatabaseFactory(context))
            }
            val libraryRoot = remember {
                File(context.filesDir, "library").apply { mkdirs() }
            }

            // SAF library root (OpenDocumentTree); falls back to the private directory.
            val preferences = remember { JsonPreferenceStore(File(context.filesDir, "preferences.json")) }
            var safTreeUri by remember {
                mutableStateOf(
                    preferences.getString("local_library_tree_uri", "").get()
                        .takeIf { it.isNotBlank() }?.let(Uri::parse),
                )
            }
            val root = remember(safTreeUri) {
                safTreeUri
                    ?.let { uri ->
                        if (app.miyomu.local.SafLocalFileRef.isAccessible(context, uri)) {
                            app.miyomu.local.SafLocalFileRef.forTree(context, uri)
                        } else null
                    }
                    ?: FileLocalFileRef(libraryRoot)
            }
            val directoryPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                    preferences.getString("local_library_tree_uri", "").set(uri.toString())
                    safTreeUri = uri
                }
            }
            val pickLibraryDirectory: () -> Unit = { directoryPicker.launch(null) }
            val comics = remember { FileLocalComicRepository() }
            val library = remember { LocalLibraryRepository() }
            val sourceInstalls = remember {
                FileSourceInstallManager(Path(File(context.filesDir, "sources").absolutePath))
            }
            val scope = rememberCoroutineScope()
            val sourceRuntime = remember { FileSourceRuntimeManager(sourceInstalls, scope) }
            val downloadStore = remember { FileDownloadStore(File(context.filesDir, "downloads")) }
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
                deviceId = "android",
                nowMillis = System::currentTimeMillis,
                preferenceStore = preferences,
                sourceInstalls = sourceInstalls,
                sourceRuntime = sourceRuntime,
                onPickLibraryDirectory = pickLibraryDirectory,
                downloadQueue = downloadQueue,
                downloadStore = downloadStore,
                backupFile = java.io.File(context.filesDir, "backup.json"),
            )
        }
    }
}
