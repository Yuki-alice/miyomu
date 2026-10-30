package app.miyomu.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.local.LocalBook
import app.miyomu.model.LibraryItem
import app.miyomu.ui.MiyomuGraph
import eu.kanade.core.preference.asState
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.library.components.LibraryToolbarTitle
import eu.kanade.presentation.library.manga.MangaLibraryContent
import eu.kanade.presentation.library.manga.MangaLibrarySettingsDialog
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryItem
import eu.kanade.tachiyomi.ui.library.manga.MangaLibrarySettingsScreenModel
import kotlinx.coroutines.launch
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Library screen built on the ported Aniyomi content layer.
 *
 * Miyomu has no category system yet, so there is exactly one synthetic category and the tab strip
 * stays hidden. Everything else (pager, grids, list, settings dialog, pull-to-refresh) is the
 * upstream implementation, fed by an adapter over the scanned local books.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MangaLibraryScreen(
    graph: MiyomuGraph,
    onOpenBook: (LocalBook) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val books by graph.library.books.collectAsState()
    val preferences = graph.libraryPreferences

    var searchQuery by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val selection = remember { mutableStateListOf<LibraryManga>() }

    // System "Default" tab plus user categories from the v3 schema; memberships filter per tab.
    val userCategories by graph.data.categories.observeCategories().collectAsState(initial = emptyList())
    var memberships by remember { mutableStateOf(emptyList<Pair<String, Long>>()) }
    var membershipsRefresh by remember { mutableStateOf(0) }
    LaunchedEffect(membershipsRefresh) {
        memberships = runCatching { graph.data.categories.allMemberships() }.getOrDefault(emptyList())
    }
    val categories = remember(userCategories) {
        listOf(Category(id = 0L, name = "", order = 0L, flags = 0L, hidden = false)) + userCategories
    }
    var currentPage by remember { mutableStateOf(0) }
    var showBulkCategoryDialog by remember { mutableStateOf(false) }

    val displayModeState = remember(scope) { preferences.displayMode().asState(scope) }
    val portraitColumns = remember(scope) { preferences.mangaPortraitColumns().asState(scope) }
    val landscapeColumns = remember(scope) { preferences.mangaLandscapeColumns().asState(scope) }

    // Progress is per book; a local book is a single readable unit, so one query per book is
    // enough to drive the read/unread badges and the continue-reading button.
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

    val itemsForCategory: (Int) -> List<MangaLibraryItem> = { page ->
        val categoryId = categories.getOrNull(page)?.id ?: 0L
        if (categoryId == 0L) {
            visibleItems
        } else {
            visibleItems.filter { item ->
                val book = books.firstOrNull { it.mangaId() == item.libraryManga.id }
                val itemId = book?.let { LibraryItem.idOf("local", it.id) }
                itemId != null && memberships.any { it.first == itemId && it.second == categoryId }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibraryToolbar(
            hasActiveFilters = false,
            selectedCount = selection.size,
            title = LibraryToolbarTitle(
                text = stringResource(MR.strings.label_library),
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
            onClickRefresh = { scope.launch { graph.library.refresh(graph.root) } },
            // Local books have no remote source to update; refreshing rescans the directory.
            onClickGlobalUpdate = {
                scope.launch { graph.library.refresh(graph.root) }
                graph.libraryUpdater.checkNow()
            },
            onClickOpenRandomEntry = {
                allItems.randomOrNull()?.let { item ->
                    books.firstOrNull { it.mangaId() == item.libraryManga.id }?.let(onOpenBook)
                }
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            scrollBehavior = null,
        )

        if (selection.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { showBulkCategoryDialog = true }) {
                    Text("编辑分类")
                }
            }
        }

        MangaLibraryContent(
            categories = categories,
            searchQuery = searchQuery,
            selection = selection,
            contentPadding = PaddingValues(),
            currentPage = { currentPage },
            hasActiveFilters = false,
            showPageTabs = categories.size > 1,
            onChangeCurrentPage = { currentPage = it },
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
                scope.launch { graph.library.refresh(graph.root) }
                true
            },
            onGlobalSearchClicked = {},
            getNumberOfMangaForCategory = { category -> itemsForCategory(category.id.toInt()).size },
            getDisplayMode = { displayModeState },
            getColumnsForOrientation = { isLandscape ->
                if (isLandscape) landscapeColumns else portraitColumns
            },
            getLibraryForPage = { page -> itemsForCategory(page) },
        )
    }

    if (showBulkCategoryDialog) {
        CategoryEditDialog(
            categories = userCategories,
            memberOf = emptySet(),
            onDismiss = { showBulkCategoryDialog = false },
            onConfirm = { selectedIds_, newName ->
                scope.launch {
                    var targetIds = selectedIds_
                    if (newName != null) {
                        val id = graph.data.categories.upsert(
                            Category(id = Category.UNCATEGORIZED_ID, name = newName, order = userCategories.size.toLong()),
                        )
                        targetIds = selectedIds_ + id
                    }
                    val itemIds = selection.mapNotNull { libraryManga ->
                        books.firstOrNull { it.mangaId() == libraryManga.id }
                            ?.let { LibraryItem.idOf("local", it.id) }
                    }
                    itemIds.forEach { itemId -> graph.data.categories.setItemCategories(itemId, targetIds) }
                    selection.clear()
                    membershipsRefresh++
                }
                showBulkCategoryDialog = false
            },
        )
    }

    if (showSettings) {
        // Built on demand: the screen model starts a Voyager scope, which should not run until
        // the dialog is actually open.
        val settingsModel = remember(preferences) { MangaLibrarySettingsScreenModel(preferences) }
        MangaLibrarySettingsDialog(
            onDismissRequest = { showSettings = false },
            screenModel = settingsModel,
            category = null,
        )
    }
}
