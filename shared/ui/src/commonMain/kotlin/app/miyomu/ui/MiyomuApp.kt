package app.miyomu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.model.MediaType
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
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
    onOpenLibraryFolder: (() -> Unit)? = null,
    novelFetcher: (suspend (String) -> String)? = null,
) {
    val store = preferenceStore ?: remember { InMemoryPreferenceStore() }

    val graph = remember {
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
            onOpenLibraryFolder = onOpenLibraryFolder,
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

    MiyomuTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Navigator(HomeScreen(graph))
        }
    }
}
