package app.miyomu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.miyomu.data.MiyomuData
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.ThemeMode
import app.miyomu.download.DownloadQueue
import app.miyomu.download.FileDownloadStore
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.model.MediaType
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
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
    novels: LocalNovelRepository,
    novelLibrary: NovelLibraryRepository,
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
    onPickLibraryDirectory: (() -> Unit)? = null,
    novelFetcher: (suspend (String) -> String)? = null,
) {
    val store = preferenceStore ?: remember { InMemoryPreferenceStore() }

    // The graph rebuilds when the library root changes (SAF directory switch); source runtimes
    // reload on the next access, which is acceptable for this rare operation.
    val graph = remember(root.location, store) {
        MiyomuGraph(
            library = library,
            comics = comics,
            novels = novels,
            novelLibrary = novelLibrary,
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
            onPickLibraryDirectory = onPickLibraryDirectory,
            novelFetcher = novelFetcher,
        )
    }

    // Coil resolves covers through a process-wide singleton, so the local fetcher has to be
    // registered before the first grid item composes. Comic and novel books share the reserved
    // local source id; the resolver branches on the media type to fetch the right bytes.
    remember(graph) {
        installLocalCoverLoader(
            localCoverResolver(
                books = { graph.library.books.value + graph.novelLibrary.books.value },
                cover = { book ->
                    if (book.mediaType == MediaType.NOVEL) {
                        graph.novels.cover(book)
                    } else {
                        graph.comics.cover(book)
                    }
                },
            ),
        )
    }

    LaunchedEffect(root) {
        library.refresh(root)
        novelLibrary.refresh(root)
    }

    // pref_theme_mode_key historically held 0/1/2 ints; new writes use the enum name.
    val themeModeStored = remember(store) {
        store.getString("pref_theme_mode_key", ThemeMode.SYSTEM.name).changes()
    }.collectAsState(initial = ThemeMode.SYSTEM.name)
    val appThemeStored = remember(store) {
        store.getString("pref_app_theme", "").changes()
    }.collectAsState(initial = "")
    val amoledState = remember(store) {
        store.getBoolean("pref_theme_dark_amoled_key", false).changes()
    }.collectAsState(initial = store.getBoolean("pref_theme_dark_amoled_key", false).get())

    MiyomuTheme(
        themeMode = ThemeMode.fromStored(themeModeStored.value),
        appTheme = AppTheme.fromStored(appThemeStored.value, ThemeMode.fromStored(themeModeStored.value)),
        amoled = amoledState.value,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Navigator(HomeScreen(graph))
        }
    }
}

