@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.miyomu.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.download.DownloadQueueState
import app.miyomu.download.DownloadStatus
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.HeadingItem

/** Download settings: queue state plus destructive cleanups bound to the real queue/store. */
@Composable
fun SettingsDownloadRoute(graph: MiyomuGraph) {
        val navigator = LocalMiyomuNavigator.current
        val scope = rememberCoroutineScope()
        val queue = graph.downloadQueue
        val store = graph.downloadStore
        val state = if (queue != null) {
            val s by queue.state.collectAsState()
            s
        } else {
            remember { DownloadQueueState() }
        }
        var status by remember { mutableStateOf<String?>(null) }
        var showDeleteAllConfirm by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("下载") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { HeadingItem("队列") }
                if (queue == null) {
                    item {
                        Text(
                            text = "此平台未启用下载队列",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "排队 ${state.activeOrQueued.size} · 共 ${state.tasks.size} 条记录",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.tasks.any { it.status == DownloadStatus.RUNNING }) {
                        item {
                            val running = state.tasks.firstOrNull { it.status == DownloadStatus.RUNNING }
                            LinearProgressIndicator(
                                progress = {
                                    if (running != null && running.pagesTotal > 0) {
                                        running.pagesDone.toFloat() / running.pagesTotal
                                    } else 0f
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    item {
                        TextButton(
                            onClick = {
                                state.tasks
                                    .filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }
                                    .forEach { queue.cancel(it.id) }
                                status = "已取消全部排队任务"
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) { Text("取消全部排队任务") }
                    }
                    item {
                        TextButton(
                            onClick = {
                                queue.clearFinished()
                                status = "已清除完成的记录（文件保留）"
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) { Text("清除已完成记录") }
                    }
                }
                item { HeadingItem("已下载内容") }
                item {
                    TextButton(
                        onClick = { showDeleteAllConfirm = true },
                        enabled = store != null,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) { Text("删除全部下载") }
                }
                status?.let {
                    item {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }

        if (showDeleteAllConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteAllConfirm = false },
                title = { Text("删除全部下载") },
                text = { Text("将删除所有已下载的章节文件，此操作不可恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        store?.clearAll()
                        status = "已删除全部下载"
                        showDeleteAllConfirm = false
                    }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllConfirm = false }) { Text("取消") }
                },
            )
        }
    }

private data class Cleanup(
    val id: String,
    val label: String,
    val action: suspend (MiyomuGraph) -> Unit,
)

private val cleanups = listOf(
    Cleanup("history", "清除阅读历史") { it.data.progress.clearAll() },
    Cleanup("marks", "清除章节已读/书签标记") {
        it.data.chapters.clearAll()
        it.data.anime.clearAll()
    },
    Cleanup("updates", "清除更新记录") { it.data.libraryUpdates.clear() },
    Cleanup("resolve", "清除视频解析缓存") { it.data.videoResolve.clearAll() },
)

@Composable
fun SettingsDataRoute(graph: MiyomuGraph) {
        val navigator = LocalMiyomuNavigator.current
        val scope = rememberCoroutineScope()
        var status by remember { mutableStateOf<String?>(null) }
        var pending by remember { mutableStateOf<Cleanup?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("数据与存储") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                item { HeadingItem("清除数据") }
                items(cleanups.size) { index ->
                    val cleanup = cleanups[index]
                    TextButton(
                        onClick = { pending = cleanup },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) { Text(cleanup.label) }
                }
                status?.let {
                    item {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }

        pending?.let { cleanup ->
            AlertDialog(
                onDismissRequest = { pending = null },
                title = { Text(cleanup.label) },
                text = { Text("此操作不可恢复，确认继续？") },
                confirmButton = {
                    TextButton(onClick = {
                        pending = null
                        scope.launch {
                            runCatching { cleanup.action(graph) }
                                .onSuccess { status = "${cleanup.label} 完成" }
                                .onFailure { status = "${cleanup.label} 失败：${it.message}" }
                        }
                    }) { Text("清除") }
                },
                dismissButton = {
                    TextButton(onClick = { pending = null }) { Text("取消") }
                },
            )
        }
    }

