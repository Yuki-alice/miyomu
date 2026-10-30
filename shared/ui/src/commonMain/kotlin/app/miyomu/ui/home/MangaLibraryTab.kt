package app.miyomu.ui.home

import androidx.compose.runtime.Composable
import app.miyomu.local.LocalBook
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.library.MangaLibraryScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Library tab backed by the ported Aniyomi library screens (ADR-0011, A2-3).
 */
@Composable
fun MangaLibraryTab(
    graph: MiyomuGraph,
    onOpenBook: (LocalBook) -> Unit,) {



        MangaLibraryScreen(graph = graph, onOpenBook = onOpenBook)
    }
