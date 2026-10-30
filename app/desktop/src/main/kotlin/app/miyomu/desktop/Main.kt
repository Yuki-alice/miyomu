package app.miyomu.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.miyomu.application.MiyomuAppEnvironment
import app.miyomu.application.MiyomuApplication
import app.miyomu.application.MiyomuServices
import app.miyomu.data.JvmMiyomuDatabaseFactory
import app.miyomu.local.FileLocalFileRef
import app.miyomu.model.MediaType
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val COMIC_EXTENSIONS = setOf("zip", "cbz", "7z", "cb7", "pdf")
private val NOVEL_EXTENSIONS = setOf("txt", "epub", "fb2", "html", "htm", "md", "markdown", "mobi", "azw3")

fun main(args: Array<String>) {
    // Services are created once, outside any composition, and closed when the window exits;
    // relayouts or a recreated window state never restart the database or the download queue.
    val home = File(System.getProperty("user.home"), ".miyomu").apply { mkdirs() }
    val libraryRoot = File(home, "library").apply { mkdirs() }
    val services = MiyomuServices.create(
        dataDir = home,
        databaseFactory = JvmMiyomuDatabaseFactory(File(home, "miyomu.db").absolutePath),
    )
    // P1-6 系统集成: `miyomu <file>` (desktop file association) imports a supported book into
    // the library root and opens its detail page; the first scan shelves it.
    val openedBook = args.firstOrNull()?.let { path -> importExternalBook(File(path), libraryRoot) }
    application {
        Window(
            onCloseRequest = {
                services.close()
                exitApplication()
            },
            title = "Miyomu",
            state = rememberWindowState(width = 1200.dp, height = 820.dp),
        ) {
            val environment = remember(libraryRoot, openedBook) {
                MiyomuAppEnvironment(
                    dataDir = home,
                    libraryRoot = FileLocalFileRef(libraryRoot),
                    deviceId = "desktop",
                    nowMillis = System::currentTimeMillis,
                    onOpenLibraryFolder = {
                        runCatching { Desktop.getDesktop().open(libraryRoot) }
                    },
                    startBookLocation = openedBook?.first,
                    startBookMediaType = openedBook?.second,
                    onExit = { exitApplication() },
                )
            }
            MiyomuApplication(environment, services)
        }
    }
}

/** Imports [path] into the library root; returns (location, mediaType) or null if unsupported. */
private fun importExternalBook(path: File, libraryRoot: File): Pair<String, MediaType>? {
    if (!path.isFile) return null
    val extension = path.extension.lowercase()
    val mediaType = when (extension) {
        in COMIC_EXTENSIONS -> MediaType.COMIC
        in NOVEL_EXTENSIONS -> MediaType.NOVEL
        else -> return null
    }
    val target = if (path.parentFile?.canonicalPath == libraryRoot.canonicalPath) {
        path
    } else {
        var destination = File(libraryRoot, path.name)
        var copy = 1
        while (destination.exists() && destination.length() == path.length()) {
            destination = File(libraryRoot, "${path.nameWithoutExtension} (${copy++}).$extension")
        }
        runCatching {
            Files.copy(path.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }.getOrNull() ?: return null
        destination
    }
    return target.absolutePath to mediaType
}
