package app.miyomu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.miyomu.data.MiyomuData
import app.miyomu.download.DownloadQueue
import app.miyomu.download.FileDownloadStore
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.source.manager.SourceRuntimeManager
import app.miyomu.source.repo.SourceInstallManager
import app.miyomu.ui.home.HomeScreen
import app.miyomu.ui.library.installLocalCoverLoader
import app.miyomu.ui.library.localCoverResolver
import cafe.adriel.voyager.navigator.Navigator
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore

@Composable
fun MiyomuApp(
    library: LocalLibraryRepository,
    comics: LocalComicRepository,
    data: MiyomuData,
    root: LocalFileRef,
    deviceId: String,
    nowMillis: () -> Long,
    /**
     * Backing store for the ported preference groups. Platforms pass a JSON file; tests and
     * previews fall back to an in-memory store.
     */
    preferenceStore: PreferenceStore? = null,
    sourceInstalls: SourceInstallManager,
    sourceRuntime: SourceRuntimeManager,
    downloadQueue: DownloadQueue? = null,
    downloadStore: FileDownloadStore? = null,
    backupFile: java.io.File? = null,
    onOpenLibraryFolder: (() -> Unit)? = null,
) {
    val store = preferenceStore ?: remember { InMemoryPreferenceStore() }

    val graph = remember {
        MiyomuGraph(
            library = library,
            comics = comics,
            data = data,
            root = root,
            deviceId = deviceId,
            nowMillis = nowMillis,
            preferenceStore = store,
            sourceInstalls = sourceInstalls,
            sourceRuntime = sourceRuntime,
            downloadQueue = downloadQueue,
            downloadStore = downloadStore,
            backupFile = backupFile,
            onOpenLibraryFolder = onOpenLibraryFolder,
        )
    }

    // Coil resolves covers through a process-wide singleton, so the local fetcher has to be
    // registered before the first grid item composes.
    remember(graph) {
        installLocalCoverLoader(
            localCoverResolver(
                books = { graph.library.books.value },
                cover = { book -> graph.comics.cover(book) },
            ),
        )
    }

    LaunchedEffect(root) {
        library.refresh(root)
    }

    MiyomuTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Navigator(HomeScreen(graph))
        }
    }
}
