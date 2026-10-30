@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.miyomu.ui.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.PaddingValues
import app.miyomu.data.LibraryUpdate
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Updates tab backed by the library update check (ADR-0014 B2): rows come from the
 * `library_updates` table, newest first and grouped by day, with the check progress shown while
 * a global update runs. Clicking a row resumes into the entry it belongs to.
 */
class UpdatesTab(
    private val graph: MiyomuGraph,
    private val onOpen: (itemId: String) -> Unit,
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "更新")

    @Composable
    override fun Content() {
        val updates by graph.data.libraryUpdates.observeUpdates().collectAsState(initial = emptyList())
        val checkState by graph.libraryUpdater.state.collectAsState()
        val titles = remember(updates) {
            updates.associate { update ->
                val scopeId = update.itemId.substringBefore(':')
                val sourceKey = update.itemId.substringAfter(':')
                val title = if (scopeId == "local") {
                    sourceKey
                } else {
                    kotlinx.coroutines.runBlocking {
                        runCatching { graph.data.library.get(update.itemId)?.title }.getOrNull()
                    } ?: sourceKey
                }
                update.itemId to title
            }
        }
        val covers = remember(updates, titles) {
            updates.associate { update ->
                val cover = if (update.itemId.startsWith("local:")) {
                    null
                } else {
                    kotlinx.coroutines.runBlocking {
                        runCatching { graph.data.library.get(update.itemId)?.coverUrl }.getOrNull()
                    }
                }
                update.itemId to cover
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "更新", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (!checkState.running) {
                    TextButton(onClick = { graph.libraryUpdater.checkNow() }) { Text("检查更新") }
                }
            }

            if (checkState.running) {
                Text(
                    text = "正在检查 ${checkState.checked}/${checkState.total} · 新章节 ${checkState.newChapters}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LinearProgressIndicator(
                    progress = {
                        if (checkState.total > 0) checkState.checked.toFloat() / checkState.total else 0f
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            checkState.errors.forEach { (title, message) ->
                Text(
                    text = "$title：$message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (updates.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Update,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "还没有更新记录",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = "把在线漫画加入书架后，用书架的“全局更新”检查新章节。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val grouped = remember(updates) { groupByDay(updates) }
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    grouped.forEach { (date, rows) ->
                        item(key = "header-$date") {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        itemsIndexed(rows, key = { _, row -> "${row.itemId}:${row.key}" }) { _, row ->
                            UpdateRow(
                                title = titles[row.itemId] ?: row.itemId,
                                coverUrl = covers[row.itemId],
                                chapterName = row.name,
                                onClick = { onOpen(row.itemId) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

private fun groupByDay(updates: List<LibraryUpdate>): List<Pair<LocalDate, List<LibraryUpdate>>> =
    updates
        .groupBy { LocalDate.from(Instant.ofEpochMilli(it.fetchedAt).atZone(ZoneId.systemDefault())) }
        .toSortedMap(compareByDescending { it })
        .map { (date, rows) -> date to rows }

@Composable
private fun UpdateRow(
    title: String,
    coverUrl: String?,
    chapterName: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .height(88.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(56.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = chapterName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
