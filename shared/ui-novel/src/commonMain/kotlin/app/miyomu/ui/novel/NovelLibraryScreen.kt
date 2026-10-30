package app.miyomu.ui.novel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.library.mangaId
import app.miyomu.ui.library.toLibraryItem
import eu.kanade.core.preference.asState
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.presentation.library.manga.MangaLibraryContent
import eu.kanade.presentation.library.manga.MangaLibrarySettingsDialog
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryItem
import eu.kanade.tachiyomi.ui.library.manga.MangaLibrarySettingsScreenModel
import kotlinx.coroutines.launch
import app.miyomu.model.Category
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Novel library built on the same ported Aniyomi content layer as the manga library
 * (ADR-0011): novels synthesize into [tachiyomi.domain.entries.manga.model.Manga] exactly
 * like [app.miyomu.ui.library.MangaLibraryScreen] does for comics.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NovelLibraryScreen(
    graph: MiyomuGraph,
    onOpenBook: (LocalBook) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val books by graph.novelLibrary.books.collectAsState()
    val preferences = graph.libraryPreferences

    var searchQuery by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<LibraryManga>() }

    val categories = remember { listOf(Category(id = 0L, name = "", order = 0L, flags = 0L, hidden = false)) }

    val displayModeState = remember(scope) { preferences.displayMode().asState(scope) }
    val portraitColumns = remember(scope) { preferences.mangaPortraitColumns().asState(scope) }
    val landscapeColumns = remember(scope) { preferences.mangaLandscapeColumns().asState(scope) }

    val allItems by produceState(initialValue = emptyList<MangaLibraryItem>(), books) {
        value = books.map { book ->
            val progress = graph.data.progress.latest(LibraryItem.idOf("local", book.location))
            book.toLibraryItem(progress)
        }
    }

    val visibleItems = remember(allItems, searchQuery) {
        val query = searchQuery
        if (query.isNullOrBlank()) allItems else allItems.filter { it.matches(query) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibraryToolbar(
            hasActiveFilters = false,
            selectedCount = selection.size,
            title = LibraryToolbarTitle(
                text = stringResource(MR.strings.label_novels),
                numberOfEntries = allItems.size,
            ),
            onClickUnselectAll = { selection.clear() },
            onClickSelectAll = { selection.clear(); selection.addAll(visibleItems.map { it.libraryManga }) },
            onClickInvertSelection = {
                val inverted = visibleItems.map { it.libraryManga }.filterNot { candidate ->
                    selection.any { it.id == candidate.id }
                }
                selection.clear()
                selection.addAll(inverted)
            },
            onClickFilter = { showSettings = true },
            onClickRefresh = { scope.launch { graph.novelLibrary.refresh(graph.root) } },
            onClickGlobalUpdate = { scope.launch { graph.novelLibrary.refresh(graph.root) } },
            onClickOpenRandomEntry = {
                allItems.randomOrNull()?.let { item ->
                    books.firstOrNull { it.mangaId() == item.libraryManga.id }?.let(onOpenBook)
                }
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollBehavior = null,
        )

        if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(MR.strings.novel_empty_library))
            }
        } else {
            MangaLibraryContent(
                categories = categories,
                searchQuery = searchQuery,
                selection = selection,
                contentPadding = PaddingValues(),
                currentPage = { 0 },
                hasActiveFilters = false,
                showPageTabs = false,
                onChangeCurrentPage = {},
                onMangaClicked = { mangaId ->
                    books.firstOrNull { it.mangaId() == mangaId }?.let(onOpenBook)
                },
                onContinueReadingClicked = { libraryManga ->
                    books.firstOrNull { it.mangaId() == libraryManga.id }?.let(onOpenBook)
                },
                onToggleSelection = { libraryManga ->
                    if (selection.any { it.id == libraryManga.id }) {
                        selection.removeAll { it.id == libraryManga.id }
                    } else {
                        selection.add(libraryManga)
                    }
                },
                onToggleRangeSelection = { libraryManga ->
                    if (selection.any { it.id == libraryManga.id }) {
                        selection.removeAll { it.id == libraryManga.id }
                    } else {
                        selection.add(libraryManga)
                    }
                },
                onRefresh = {
                    scope.launch { graph.novelLibrary.refresh(graph.root) }
                    true
                },
                onGlobalSearchClicked = {},
                getNumberOfMangaForCategory = { allItems.size },
                getDisplayMode = { displayModeState },
                getColumnsForOrientation = { isLandscape ->
                    if (isLandscape) landscapeColumns else portraitColumns
                },
                getLibraryForPage = { visibleItems },
            )
        }
    }

    if (showSettings) {
        val settingsModel = remember(preferences) { MangaLibrarySettingsScreenModel(preferences, scope) }
        MangaLibrarySettingsDialog(
            onDismissRequest = { showSettings = false },
            screenModel = settingsModel,
            category = null,
        )
    }
}
