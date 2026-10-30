@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.miyomu.ui.history

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.ui.MiyomuGraph
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.history.manga.MangaHistoryDeleteDialogHost
import eu.kanade.presentation.history.manga.MangaHistoryScreen
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

/**
 * Reading history tab, backed by the ported Aniyomi `MangaHistoryScreen` (ADR-0011 pattern:
 * ported presentation + Miyomu screen model composing the progress table).
 */
@Composable
fun MangaHistoryTab(
    graph: MiyomuGraph,
    onResume: (itemId: String, entryId: String) -> Unit,
    onOpenDetails: (itemId: String) -> Unit,) {



        val scope = rememberCoroutineScope()
        val model = remember(graph) { MangaHistoryScreenModelMiyomu(graph, scope) }
        val state by model.state.collectAsState()
        var searchQuery by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) { model.refresh() }

        androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
            SearchToolbar(
                searchQuery = searchQuery,
                onChangeSearchQuery = {
                    searchQuery = it
                    model.search(it)
                },
                placeholderText = "搜索历史",
                modifier = Modifier.padding(horizontal = 0.dp),
            )
            MangaHistoryScreen(
                state = eu.kanade.presentation.history.manga.MangaHistoryState(list = state.list),
                snackbarHostState = remember { SnackbarHostState() },
                onClickCover = { history ->
                    model.resolveIds(history)?.let { (_, itemId) -> onOpenDetails(itemId) }
                },
                onClickResume = { history ->
                    model.resolveIds(history)?.let { (itemId, entryId) -> onResume(itemId, entryId) }
                },
                onClickFavorite = { /* local books are always in the library; online favorites use the detail screen */ },
                onDialogChange = model::onDialogChange,
                searchQuery = searchQuery,
            )
        }

        MangaHistoryDeleteDialogHost(
            dialog = state.dialog,
            onDismiss = { model.onDialogChange(null) },
            onDelete = { history, removeEverything -> model.deleteHistory(history, removeEverything) },
        )
    }


@Composable
private fun rememberCoroutineScopeCompat(): kotlinx.coroutines.CoroutineScope =
    androidx.compose.runtime.rememberCoroutineScope()
