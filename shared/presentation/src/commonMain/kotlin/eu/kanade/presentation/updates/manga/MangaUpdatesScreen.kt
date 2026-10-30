/*
 * Copyright 2026 Miyomu contributors
 *
 * Ported from Aniyomi (https://github.com/aniyomiorg/aniyomi), Copyright Aniyomi contributors,
 * licensed under the Apache License, Version 2.0. Bindings changed for Miyomu: screen state,
 * dialog and item types are defined here instead of the app-module screen model, and the
 * download-state providers report NOT_DOWNLOADED until chapter downloads cover the feed.
 */
package eu.kanade.presentation.updates.manga

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.material.PullRefresh
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entries.components.EntryBottomActionMenu
import eu.kanade.presentation.updates.manga.components.MangaUpdatesUiItem
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.tachiyomi.data.download.manga.model.MangaDownload
import tachiyomi.domain.updates.manga.model.MangaUpdatesWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate

/** Miyomu binding: update rows carry the raw string ids for navigation through the side index. */
data class MangaUpdatesItem(
    val update: MangaUpdatesWithRelations,
    val itemId: String,
    val entryKey: String,
    val selected: Boolean = false,
)

data class MangaUpdatesState(
    val isLoading: Boolean = true,
    val items: List<MangaUpdatesItem> = emptyList(),
) {
    val selected: List<MangaUpdatesItem> get() = items.filter { it.selected }
    val selectionMode: Boolean get() = selected.isNotEmpty()

    fun getUiModel(): List<MangaUpdatesUiModel> =
        items
            .map { MangaUpdatesUiModel.Item(it) }
            .let { list ->
                list.foldIndexed(mutableListOf<MangaUpdatesUiModel>()) { index, acc, entry ->
                    val currentDate = entry.item.update.dateFetch
                        .let { if (it > 0) java.time.Instant.ofEpochMilli(it) else null }
                        ?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
                    val previous = acc.lastOrNull()
                    val previousDate = (previous as? MangaUpdatesUiModel.Item)
                        ?.item?.update?.dateFetch
                        ?.let { if (it > 0) java.time.Instant.ofEpochMilli(it) else null }
                        ?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
                    if (index == 0 && currentDate != null) {
                        acc.add(MangaUpdatesUiModel.Header(currentDate))
                    } else if (previousDate != currentDate && currentDate != null) {
                        acc.add(MangaUpdatesUiModel.Header(currentDate))
                    }
                    acc.add(entry)
                    acc
                }
            }
}

sealed interface MangaUpdatesUiModel {
    data class Header(val date: LocalDate) : MangaUpdatesUiModel
    data class Item(val item: MangaUpdatesItem) : MangaUpdatesUiModel
}

@Composable
fun MangaUpdateScreen(
    state: MangaUpdatesState,
    snackbarHostState: SnackbarHostState,
    lastUpdated: Long,
    onClickCover: (MangaUpdatesItem) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    onUpdateLibrary: () -> Boolean,
    onMultiBookmarkClicked: (List<MangaUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<MangaUpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<MangaUpdatesItem>) -> Unit,
    onUpdateSelected: (MangaUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onOpenChapter: (MangaUpdatesItem) -> Unit,
) {
    androidx.compose.ui.backhandler.BackHandler(enabled = state.selectionMode, onBack = { onSelectAll(false) })

    Scaffold(
        bottomBar = {
            MangaUpdatesBottomBar(
                selected = state.selected,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMultiDeleteClicked = onMultiDeleteClicked,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
            state.items.isEmpty() -> EmptyScreen(
                stringRes = MR.strings.information_no_recent,
                modifier = Modifier.padding(contentPadding),
            )
            else -> {
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                var isRefreshing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                PullRefresh(
                    refreshing = isRefreshing,
                    onRefresh = {
                        val started = onUpdateLibrary()
                        if (!started) return@PullRefresh
                        scope.launch {
                            // Fake refresh status but hide it after a second as it's a long task
                            isRefreshing = true
                            kotlinx.coroutines.delay(1_000L)
                            isRefreshing = false
                        }
                    },
                    enabled = !state.selectionMode,
                    indicatorPadding = contentPadding,
                ) {
                    FastScrollLazyColumn(
                        contentPadding = contentPadding,
                    ) {
                        mangaUpdatesLastUpdatedItem(lastUpdated)

                        mangaUpdatesUiItems(
                            uiModels = state.getUiModel(),
                            selectionMode = state.selectionMode,
                            onUpdateSelected = onUpdateSelected,
                            onClickCover = onClickCover,
                            onClickUpdate = onOpenChapter,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaUpdatesBottomBar(
    selected: List<MangaUpdatesItem>,
    onMultiBookmarkClicked: (List<MangaUpdatesItem>, bookmark: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<MangaUpdatesItem>, read: Boolean) -> Unit,
    onMultiDeleteClicked: (List<MangaUpdatesItem>) -> Unit,
) {
    EntryBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, true)
        }.takeIf { selected.any { !it.update.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected, false)
        }.takeIf { selected.all { it.update.bookmark } },
        onMarkAsViewedClicked = {
            onMultiMarkAsReadClicked(selected, true)
        }.takeIf { selected.any { !it.update.read } },
        onMarkAsUnviewedClicked = {
            onMultiMarkAsReadClicked(selected, false)
        }.takeIf { selected.any { it.update.read || it.update.lastPageRead > 0L } },
        onDownloadClicked = null,
        onDeleteClicked = null,
        isManga = true,
    )
}

internal fun LazyListScope.mangaUpdatesLastUpdatedItem(
    lastUpdated: Long,
) {
    if (lastUpdated <= 0L) return
    item(key = "mangaUpdates-lastUpdated") {
        Box(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        ) {
            Text(
                text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

internal fun LazyListScope.mangaUpdatesUiItems(
    uiModels: List<MangaUpdatesUiModel>,
    selectionMode: Boolean,
    onUpdateSelected: (MangaUpdatesItem, Boolean, Boolean, Boolean) -> Unit,
    onClickCover: (MangaUpdatesItem) -> Unit,
    onClickUpdate: (MangaUpdatesItem) -> Unit,
) {
    items(
        items = uiModels,
        contentType = {
            when (it) {
                is MangaUpdatesUiModel.Header -> "header"
                is MangaUpdatesUiModel.Item -> "item"
            }
        },
        key = {
            when (it) {
                is MangaUpdatesUiModel.Header -> "mangaUpdatesHeader-${it.hashCode()}"
                is MangaUpdatesUiModel.Item -> "mangaUpdates-${it.item.update.mangaId}-${it.item.update.chapterId}"
            }
        },
    ) { item ->
        when (item) {
            is MangaUpdatesUiModel.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    text = relativeDateText(item.date),
                )
            }
            is MangaUpdatesUiModel.Item -> {
                val updatesItem = item.item
                MangaUpdatesUiItem(
                    modifier = Modifier.animateItemFastScroll(),
                    update = updatesItem.update,
                    selected = updatesItem.selected,
                    readProgress = null,
                    onLongClick = {
                        onUpdateSelected(updatesItem, !updatesItem.selected, true, true)
                    },
                    onClick = {
                        when {
                            selectionMode -> onUpdateSelected(
                                updatesItem,
                                !updatesItem.selected,
                                true,
                                false,
                            )
                            else -> onClickUpdate(updatesItem)
                        }
                    },
                    onClickCover = { onClickCover(updatesItem) }.takeIf { !selectionMode },
                    onDownloadChapter = null,
                    downloadStateProvider = { MangaDownload.State.NOT_DOWNLOADED },
                    downloadProgressProvider = { 0 },
                )
            }
        }
    }
}

