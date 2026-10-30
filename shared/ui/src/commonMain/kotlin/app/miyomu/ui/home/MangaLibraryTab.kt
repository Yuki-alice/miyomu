package app.miyomu.ui.home

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import app.miyomu.local.LocalBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.library.MangaLibraryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Library tab backed by the ported Aniyomi library screens (ADR-0011, A2-3).
 */
class MangaLibraryTab(
    private val graph: MiyomuGraph,
    private val onOpenBook: (LocalBook) -> Unit,
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = stringResource(MR.strings.label_library))

    @Composable
    override fun Content() {
        MangaLibraryScreen(graph = graph, onOpenBook = onOpenBook)
    }
}
