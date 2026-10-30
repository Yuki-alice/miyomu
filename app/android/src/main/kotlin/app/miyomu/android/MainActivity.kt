package app.miyomu.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.miyomu.application.MiyomuAppEnvironment
import app.miyomu.application.MiyomuApplication
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.SafLocalFileRef
import app.miyomu.model.MediaType
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val libraryDir = File(filesDir, "library").apply { mkdirs() }
        // P1-6 系统集成: import an externally opened book (ACTION_VIEW) before the first
        // composition so the initial scan shelves it and the shell can start on its detail
        // page. Blocking here is deliberate: the import happens once, during cold start.
        val openedBook = importOpenedBook(intent, libraryDir)
        setContent {
            val context = LocalContext.current.applicationContext
            val platformPrefs = remember {
                context.getSharedPreferences("miyomu-platform", Context.MODE_PRIVATE)
            }
            val services = remember { MiyomuAndroidApp.from(context).services }
            val libraryDirState = remember { libraryDir }
            var safTreeUri by remember {
                mutableStateOf(platformPrefs.getString("local_library_tree_uri", null)?.let(Uri::parse))
            }
            val root = remember(safTreeUri, libraryDirState) {
                safTreeUri
                    ?.takeIf { SafLocalFileRef.isAccessible(context, it) }
                    ?.let { SafLocalFileRef.forTree(context, it) }
                    ?: FileLocalFileRef(libraryDirState)
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
                    platformPrefs.edit().putString("local_library_tree_uri", uri.toString()).apply()
                    safTreeUri = uri
                }
            }
            val environment = remember(root, openedBook) {
                MiyomuAppEnvironment(
                    dataDir = context.filesDir,
                    libraryRoot = root,
                    deviceId = "android",
                    nowMillis = System::currentTimeMillis,
                    onPickLibraryDirectory = { directoryPicker.launch(null) },
                    startBookLocation = openedBook?.first,
                    startBookMediaType = openedBook?.second,
                    onExit = { finish() },
                )
            }
            MiyomuApplication(environment, services)
        }
    }

    /**
     * Copies the ACTION_VIEW payload into the library root and classifies it by extension.
     * Returns the resulting book location + media type, or null when the intent is not a
     * view of a supported book format. Unknown extensions are ignored rather than shelved.
     */
    private fun importOpenedBook(intent: Intent?, libraryDir: File): Pair<String, MediaType>? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        return runCatching {
            val rawName = uri.lastPathSegment.orEmpty().substringAfterLast('/').ifEmpty { "imported.book" }
            val extension = rawName.substringAfterLast('.', "").lowercase()
            val mediaType = when (extension) {
                in COMIC_EXTENSIONS -> MediaType.COMIC
                in NOVEL_EXTENSIONS -> MediaType.NOVEL
                else -> return null
            }
            val baseName = rawName.substringBeforeLast('.')
            var target = File(libraryDir, "$baseName.$extension")
            var copy = 1
            while (target.exists()) {
                target = File(libraryDir, "$baseName (${copy++}).$extension")
            }
            contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (target.length() == 0L) {
                target.delete()
                return null
            }
            target.absolutePath to mediaType
        }.getOrNull()
    }

    private companion object {
        val COMIC_EXTENSIONS = setOf("zip", "cbz", "7z", "cb7", "pdf")
        val NOVEL_EXTENSIONS = setOf("txt", "epub", "fb2", "html", "htm", "md", "markdown", "mobi", "azw3")
    }
}
