package app.miyomu.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.source.manager.SourceRuntimeInfo
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kotlinx.coroutines.launch

/** Source tab: installed JS sources and their entry points. */
class BrowseTab(
    private val graph: MiyomuGraph,
    private val onOpenSource: (SourceRuntimeInfo) -> Unit,
    private val onManageSources: () -> Unit,
    private val onOpenGlobalSearch: () -> Unit = {},
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "浏览")

    @Composable
    override fun Content() {
        val runtime = graph.sourceRuntime
        val state by runtime.state.collectAsState()
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "浏览", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenGlobalSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "全局搜索")
                }
                IconButton(onClick = { scope.launch { runtime.refresh() } }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新源")
                }
                IconButton(onClick = onManageSources) {
                    Icon(Icons.Outlined.Extension, contentDescription = "源仓库")
                }
            }

            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.sources.isEmpty() && !state.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(
                            text = "还没有可用的源",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "在“源仓库”里添加索引 JSON 并安装源。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onManageSources) { Text("打开源仓库") }
                    }
                }
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.sources, key = { it.id }) { info ->
                    SourceRow(info = info, onClick = { onOpenSource(info) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                state.errors.forEach { (id, message) ->
                    item(key = "error-$id") {
                        Text(
                            text = "$id：$message",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceRow(info: SourceRuntimeInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(info.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOfNotNull(
                    "v${info.version}",
                    info.lang.takeIf { it.isNotBlank() },
                    info.mediaType.lowercase(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "搜索",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
