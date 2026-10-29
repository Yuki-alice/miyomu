package app.miyomu.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import app.miyomu.local.LocalBook
import app.miyomu.ui.LibraryScreen
import app.miyomu.ui.MiyomuGraph
import kotlinx.coroutines.launch

class MangaLibraryTab(
    private val graph: MiyomuGraph,
    private val onOpenBook: (LocalBook) -> Unit,
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "书架")

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val books by graph.library.books.collectAsState()

        LibraryScreen(
            books = books,
            libraryPath = graph.root.location,
            comics = graph.comics,
            onOpen = onOpenBook,
            onRefresh = { scope.launch { graph.library.refresh(graph.root) } },
            onOpenLibraryFolder = graph.onOpenLibraryFolder,
        )
    }
}
