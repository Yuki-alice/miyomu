package app.miyomu.ui.home

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.novel.NovelLibraryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Novel library tab: the novel counterpart of Aniyomi's AnimeLibraryTab /
 * MangaLibraryTab split — each medium owns its library tab.
 */
@Composable
fun NovelLibraryTab(
    graph: MiyomuGraph,
    onOpenBook: (LocalBook) -> Unit,) {



        NovelLibraryScreen(graph = graph, onOpenBook = onOpenBook)
    }
