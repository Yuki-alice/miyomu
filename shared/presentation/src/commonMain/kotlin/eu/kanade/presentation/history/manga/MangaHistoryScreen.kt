/*
 * Copyright 2026 Miyomu contributors
 *
 * Ported from Aniyomi (https://github.com/aniyomiorg/aniyomi), Copyright Aniyomi contributors,
 * licensed under the Apache License, Version 2.0. Bindings changed for Miyomu: the screen state
 * and dialog types are defined here instead of on the app-module screen model, and readAt is an
 * epoch millis Long.
 */
package eu.kanade.presentation.history.manga

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.history.HistoryDeleteDialog
import eu.kanade.presentation.history.manga.components.MangaHistoryItem
import eu.kanade.presentation.util.animateItemFastScroll
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.LocalDate

/** Miyomu binding: screen state lives outside the app-module screen model (ADR-0011). */
data class MangaHistoryState(
    val list: List<MangaHistoryUiModel>? = null,
)

sealed interface MangaHistoryUiModel {
    data class Header(val date: LocalDate) : MangaHistoryUiModel
    data class Item(val item: MangaHistoryWithRelations) : MangaHistoryUiModel
}

/** Miyomu binding: dialog intents mirror the upstream screen model's sealed class. */
sealed interface MangaHistoryDialog {
    data class Delete(val history: MangaHistoryWithRelations) : MangaHistoryDialog
}

@Composable
fun MangaHistoryScreen(
    state: MangaHistoryState,
    snackbarHostState: SnackbarHostState,
    onClickCover: (history: MangaHistoryWithRelations) -> Unit,
    onClickResume: (history: MangaHistoryWithRelations) -> Unit,
    onClickFavorite: (history: MangaHistoryWithRelations) -> Unit,
    onDialogChange: (MangaHistoryDialog?) -> Unit,
    searchQuery: String? = null,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        state.list.let {
            if (it == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else if (it.isEmpty()) {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    MR.strings.no_results_found
                } else {
                    MR.strings.information_no_recent_manga
                }
                EmptyScreen(
                    stringRes = msg,
                    modifier = Modifier.padding(contentPadding),
                )
            } else {
                MangaHistoryScreenContent(
                    history = it,
                    contentPadding = contentPadding,
                    onClickCover = onClickCover,
                    onClickResume = onClickResume,
                    onClickDelete = { history -> onDialogChange(MangaHistoryDialog.Delete(history)) },
                    onClickFavorite = onClickFavorite,
                )
            }
        }
    }
}

@Composable
private fun MangaHistoryScreenContent(
    history: List<MangaHistoryUiModel>,
    contentPadding: PaddingValues,
    onClickCover: (MangaHistoryWithRelations) -> Unit,
    onClickResume: (MangaHistoryWithRelations) -> Unit,
    onClickDelete: (MangaHistoryWithRelations) -> Unit,
    onClickFavorite: (MangaHistoryWithRelations) -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        items(
            items = history,
            key = { "history-${it.hashCode()}" },
            contentType = {
                when (it) {
                    is MangaHistoryUiModel.Header -> "header"
                    is MangaHistoryUiModel.Item -> "item"
                }
            },
        ) { item ->
            when (item) {
                is MangaHistoryUiModel.Header -> {
                    ListGroupHeader(
                        modifier = Modifier.animateItemFastScroll(),
                        text = relativeDateText(item.date),
                    )
                }
                is MangaHistoryUiModel.Item -> {
                    val value = item.item
                    MangaHistoryItem(
                        modifier = Modifier.animateItemFastScroll(),
                        history = value,
                        onClickCover = { onClickCover(value) },
                        onClickResume = { onClickResume(value) },
                        onClickDelete = { onClickDelete(value) },
                        onClickFavorite = { onClickFavorite(value) },
                    )
                }
            }
        }
    }
}

/** Ported from the upstream delete dialog wiring; shown by the Miyomu history tab. */
@Composable
fun MangaHistoryDeleteDialogHost(
    dialog: MangaHistoryDialog?,
    onDismiss: () -> Unit,
    onDelete: (MangaHistoryWithRelations, removeEverything: Boolean) -> Unit,
) {
    when (dialog) {
        is MangaHistoryDialog.Delete -> HistoryDeleteDialog(
            onDismissRequest = onDismiss,
            onDelete = { removeEverything ->
                onDelete(dialog.history, removeEverything)
                onDismiss()
            },
            isManga = true,
        )
        null -> Unit
    }
}
