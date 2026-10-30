package app.miyomu.ui.novel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.source.novel.LegadoBookSource
import app.miyomu.source.novel.LegadoOnlineBook
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Browse-tab section for the legado online flow: imported sources, the online shelf and
 * the JSON import dialog. Hidden entirely when the platform did not provide a fetcher.
 */
@Composable
fun NovelSourcesSection(
    graph: MiyomuGraph,
    onOpenScreen: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (graph.novelFetcher == null) return

    val sources by graph.novelSourceStore.sources.collectAsState()
    val shelf by graph.novelSourceStore.shelf.collectAsState()
    var importVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.novel_sources),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { importVisible = true }) {
                Text(stringResource(MR.strings.novel_source_import))
            }
        }

        if (sources.isEmpty()) {
            Text(
                text = stringResource(MR.strings.novel_source_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        sources.forEach { source ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenScreen(NovelSourceSearchScreen(graph, source)) }
                        .padding(vertical = 4.dp),
                ) {
                    Text(source.bookSourceName ?: source.bookSourceUrl.orEmpty(), maxLines = 1)
                    Text(
                        text = source.bookSourceGroup ?: source.bookSourceUrl.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                if (source.exploreCategories().isNotEmpty()) {
                    TextButton(onClick = { onOpenScreen(NovelExploreScreen(graph, source)) }) {
                        Text(stringResource(MR.strings.novel_explore))
                    }
                }
            }
        }

        if (shelf.isNotEmpty()) {
            Text(
                text = stringResource(MR.strings.novel_shelf),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            shelf.forEach { book ->
                Text(
                    text = listOfNotNull(book.name, book.author).joinToString(" · "),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenScreen(NovelOnlineDetailScreen(graph, book)) }
                        .padding(vertical = 4.dp),
                    maxLines = 1,
                )
            }
        }
    }

    if (importVisible) {
        NovelSourceImportDialog(
            onDismiss = { importVisible = false },
            onImport = { parsed ->
                parsed.forEach(graph.novelSourceStore::addSource)
                importVisible = false
            },
        )
    }
}

@Composable
private fun NovelSourceImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<LegadoBookSource>) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.novel_source_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(text = error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    placeholder = { Text(stringResource(MR.strings.novel_source_import_hint)) },
                    minLines = 5,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = runCatching { LegadoBookSource.listFromJson(text) }
                    .getOrDefault(emptyList())
                    .filter { !it.bookSourceUrl.isNullOrBlank() }
                if (parsed.isEmpty()) {
                    error = "JSON"
                } else {
                    onImport(parsed)
                }
            }) {
                Text(stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
