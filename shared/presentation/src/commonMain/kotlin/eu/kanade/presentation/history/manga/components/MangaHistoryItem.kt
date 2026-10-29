/*
 * Copyright 2026 Miyomu contributors
 *
 * Ported from Aniyomi (https://github.com/aniyomiorg/aniyomi), Copyright Aniyomi contributors,
 * licensed under the Apache License, Version 2.0. Bindings changed for Miyomu: readAt is an
 * epoch millis Long formatted with java.time instead of the Context-based DateUtils helper.
 */
package eu.kanade.presentation.history.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entries.components.ItemCover
import eu.kanade.presentation.util.formatChapterNumber
import tachiyomi.domain.history.manga.model.MangaHistoryWithRelations
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HISTORY_ITEM_HEIGHT = 96.dp

private val ReadAtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatReadAt(epochMillis: Long?): String =
    epochMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(ReadAtFormatter) }
        .orEmpty()

@Composable
fun MangaHistoryItem(
    history: MangaHistoryWithRelations,
    onClickCover: () -> Unit,
    onClickResume: () -> Unit,
    onClickDelete: () -> Unit,
    onClickFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClickResume)
            .height(HISTORY_ITEM_HEIGHT)
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemCover.Book(
            modifier = Modifier.fillMaxHeight(),
            data = history.coverData,
            onClick = onClickCover,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = MaterialTheme.padding.medium, end = MaterialTheme.padding.small),
        ) {
            val textStyle = MaterialTheme.typography.bodyMedium
            Text(
                text = history.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = textStyle,
            )
            val readAt = remember(history.readAt) { formatReadAt(history.readAt) }
            Text(
                text = if (history.chapterNumber > -1) {
                    stringResource(
                        MR.strings.recent_manga_time,
                        formatChapterNumber(history.chapterNumber),
                        readAt,
                    )
                } else {
                    readAt
                },
                modifier = Modifier.padding(top = 4.dp),
                style = textStyle,
            )
        }

        if (!history.coverData.isMangaFavorite) {
            IconButton(onClick = onClickFavorite) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(MR.strings.add_to_library),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        IconButton(onClick = onClickDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun HistoryItemPreviewSurface(history: MangaHistoryWithRelations) {
    Surface {
        MangaHistoryItem(
            history = history,
            onClickCover = {},
            onClickResume = {},
            onClickDelete = {},
            onClickFavorite = {},
        )
    }
}
