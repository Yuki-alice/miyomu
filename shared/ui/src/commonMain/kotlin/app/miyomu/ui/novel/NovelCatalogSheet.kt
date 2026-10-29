package app.miyomu.ui.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.miyomu.reader.novel.NovelBookmark
import app.miyomu.reader.novel.NovelBookSearch
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Reader catalog sheet: chapter list (title filter, current chapter highlighted), bookmarks
 * and in-book search. Modeled after the TOC panels of Google Play Books / legado (behavior
 * reference only).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelCatalogSheet(
    chapterTitles: List<String?>,
    currentChapterIndex: Int,
    bookmarks: List<NovelBookmark>,
    search: suspend (String) -> List<NovelBookSearch.Match>,
    onJumpChapter: (Int) -> Unit,
    onJumpMatch: (NovelBookSearch.Match) -> Unit,
    onDeleteBookmark: (NovelBookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .padding(horizontal = 16.dp),
        ) {
            var query by remember { mutableStateOf("") }
            var results by remember { mutableStateOf<List<NovelBookSearch.Match>?>(null) }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(MR.strings.novel_search_in_book)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("novel-catalog-search"),
            )

            val listState = rememberLazyListState()
            LaunchedEffect(currentChapterIndex) {
                if (query.isBlank()) listState.scrollToItem(currentChapterIndex)
            }
            LaunchedEffect(query) {
                if (query.isBlank()) {
                    results = null
                } else {
                    delay(200)
                    results = search(query)
                }
            }

            val shownResults = results
            if (shownResults != null) {
                if (shownResults.isEmpty()) {
                    Text(
                        text = stringResource(MR.strings.novel_search_no_results),
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).testTag("novel-catalog-results")) {
                        items(shownResults) { match ->
                            val label = if (match.inTitle) {
                                chapterTitleLabel(chapterTitles, match.chapterIndex)
                            } else {
                                match.excerpt
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onJumpMatch(match) }
                                    .padding(vertical = 10.dp),
                            ) {
                                Text(label, maxLines = 1)
                                Text(
                                    text = chapterTitleLabel(chapterTitles, match.chapterIndex),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(MR.strings.novel_bookmarks),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                if (bookmarks.isEmpty()) {
                    Text(
                        text = stringResource(MR.strings.novel_no_bookmarks),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                } else {
                    LazyColumn {
                        items(bookmarks) { bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onJumpMatch(NovelBookSearch.Match(bookmark.chapterIndex, bookmark.offset, bookmark.excerpt, inTitle = false)) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(bookmark.excerpt, maxLines = 1)
                                    Text(
                                        text = chapterTitleLabel(chapterTitles, bookmark.chapterIndex),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onDeleteBookmark(bookmark) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(MR.strings.novel_remove_bookmark),
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(MR.strings.novel_catalog),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(2f)
                        .testTag("novel-catalog-chapters"),
                ) {
                    items(chapterTitles.size) { index ->
                        val selected = index == currentChapterIndex
                        Text(
                            text = chapterTitleLabel(chapterTitles, index),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJumpChapter(index) }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun chapterTitleLabel(chapterTitles: List<String?>, index: Int): String =
    chapterTitles.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "#${index + 1}"
