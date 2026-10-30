package app.miyomu.ui.download

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.download.DownloadQueue
import app.miyomu.download.DownloadStatus
import app.miyomu.download.DownloadTask
import app.miyomu.download.FileDownloadStore
import app.miyomu.ui.navigation.LocalMiyomuNavigator

/** Minimal download queue: per-task progress, cancel, and clear-finished, plus the cached
 * novel books (P1-2 统一下载中心). */
@Composable
fun DownloadQueueRoute(
    queue: DownloadQueue,
    novelStore: FileDownloadStore? = null,
) {
    val navigator = LocalMiyomuNavigator.current
    DownloadQueueContent(queue = queue, novelStore = novelStore, onBack = { navigator.pop() })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueContent(
    queue: DownloadQueue,
    novelStore: FileDownloadStore? = null,
    onBack: () -> Unit,
) {
    val state by queue.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("下载队列") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.tasks.any { it.status in terminalStatuses }) {
                        TextButton(onClick = { queue.clearFinished() }) { Text("清除已完成") }
                    }
                },
            )
        },
    ) { padding ->
        val cachedNovels = novelStore?.let { store ->
            produceState(emptyList<app.miyomu.download.NovelCachedBook>(), store) {
                value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    store.novelCachedBooks()
                }
            }.value
        } ?: emptyList()

        if (state.tasks.isEmpty() && cachedNovels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("没有下载任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.tasks, key = { "task-${it.id}" }) { task ->
                    DownloadTaskRow(task = task, onCancel = { queue.cancel(task.id) })
                }
                if (cachedNovels.isNotEmpty()) {
                    item(key = "novel-heading") {
                        Text(
                            text = "小说离线缓存",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(cachedNovels, key = { "novel-${it.itemId}" }) { book ->
                        NovelCachedBookRow(
                            title = book.title ?: "未命名书目",
                            chapterCount = book.chapterCount,
                            onClear = { novelStore?.deleteNovelBook(book.itemId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NovelCachedBookRow(title: String, chapterCount: Int, onClear: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                text = "已缓存 $chapterCount 章 · 可离线阅读",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClear) { Text("清除") }
    }
}

private val terminalStatuses = setOf(DownloadStatus.DONE, DownloadStatus.FAILED, DownloadStatus.CANCELLED)

@Composable
private fun DownloadTaskRow(task: DownloadTask, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.request.chapterTitle.ifBlank { task.request.chapterKey },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
                Text(
                    text = statusLine(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (task.status == DownloadStatus.QUEUED || task.status == DownloadStatus.RUNNING) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Outlined.Close, contentDescription = "取消")
                }
            }
        }
        if (task.status == DownloadStatus.RUNNING && task.pagesTotal > 0) {
            LinearProgressIndicator(
                progress = { task.pagesDone.toFloat() / task.pagesTotal },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}

private fun statusLine(task: DownloadTask): String {
    val video = task.video
    if (video != null) {
        val doneMiB = task.bytesDone / (1024.0 * 1024)
        return when (task.status) {
            DownloadStatus.QUEUED -> "排队中"
            DownloadStatus.RUNNING -> if (task.bytesTotal > 0) {
                "下载中 %.1f/%.1f MiB".format(doneMiB, task.bytesTotal / (1024.0 * 1024))
            } else {
                "下载中 %.1f MiB".format(doneMiB)
            }
            DownloadStatus.DONE -> "已完成 %.1f MiB".format(doneMiB)
            DownloadStatus.FAILED -> "失败：${task.error.orEmpty()}"
            DownloadStatus.CANCELLED -> "已取消"
        }
    }
    return when (task.status) {
        DownloadStatus.QUEUED -> "排队中"
        DownloadStatus.RUNNING -> if (task.pagesTotal > 0) "下载中 ${task.pagesDone}/${task.pagesTotal}" else "下载中…"
        DownloadStatus.DONE -> "已完成 ${task.pagesDone} 页"
        DownloadStatus.FAILED -> "失败：${task.error.orEmpty()}"
        DownloadStatus.CANCELLED -> "已取消"
    }
}
