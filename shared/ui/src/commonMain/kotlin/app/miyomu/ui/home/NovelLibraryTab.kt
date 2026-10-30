package app.miyomu.ui.home

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import app.miyomu.local.LocalBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.novel.NovelLibraryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Novel library tab (docs/12): the novel counterpart of Aniyomi's AnimeLibraryTab /
 * MangaLibraryTab split — each medium owns its library tab.
 */
class NovelLibraryTab(
    private val graph: MiyomuGraph,
    private val onOpenBook: (LocalBook) -> Unit,
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = stringResource(MR.strings.label_novels))

    @Composable
    override fun Content() {
        NovelLibraryScreen(graph = graph, onOpenBook = onOpenBook)
    }
}
