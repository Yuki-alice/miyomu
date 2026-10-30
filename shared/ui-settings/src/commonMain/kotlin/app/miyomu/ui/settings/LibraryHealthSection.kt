package app.miyomu.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.local.LibraryHealth
import app.miyomu.local.LocalLibraryMaintenance
import app.miyomu.local.OpdsCatalog
import app.miyomu.local.OpdsServerHandle
import app.miyomu.local.lanAddress
import app.miyomu.local.startOpdsServer
import app.miyomu.ui.MiyomuGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.presentation.core.components.HeadingItem

/**
 * P0-11 库体检: disk usage plus duplicate-title and missing-file detection over both shelves,
 * so the user can tidy the library without external tools.
 */
fun LazyListScope.libraryHealthItems(graph: MiyomuGraph) {
    item { HeadingItem("库体检") }
    item { LibraryHealthContent(graph) }
}

@Composable
private fun LibraryHealthContent(graph: MiyomuGraph) {
    var health by remember { mutableStateOf<LibraryHealth?>(null) }
    var confirmRescan by remember { mutableStateOf(false) }
    var rescanStatus by remember { mutableStateOf<String?>(null) }
    var mergeGroup by remember { mutableStateOf<List<app.miyomu.local.LocalBook>?>(null) }
    var confirmOpdsExport by remember { mutableStateOf(false) }
    var confirmOpdsImport by remember { mutableStateOf(false) }
    var showRemoteImport by remember { mutableStateOf(false) }
    var opdsStatus by remember { mutableStateOf<String?>(null) }
    // P1-6 OPDS 服务化: serves the export directory on the LAN while this screen is open;
    // leaving the screen stops the server.
    var opdsServer by remember { mutableStateOf<OpdsServerHandle?>(null) }
    DisposableEffect(Unit) {
        onDispose { opdsServer?.stop() }
    }
    val scope = rememberCoroutineScope()
    val comics by graph.library.books.collectAsState()
    val novels by graph.novelLibrary.books.collectAsState()

    // The walk can touch thousands of files; keep it off the composition thread.
    LaunchedEffect(comics, novels) {
        health = withContext(Dispatchers.IO) {
            LocalLibraryMaintenance().audit(comics + novels)
        }
    }

    val current = health ?: return
    Text(
        text = "存储占用：${LocalLibraryMaintenance.formatBytes(current.totalBytes)}（${current.bookCount} 本）",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
    if (current.corruptBooks.isNotEmpty()) {
        Text(
            text = "文件损坏：${current.corruptBooks.size} 本 — " +
                current.corruptBooks.joinToString("、") { it.title } +
                "（无法打开，建议删除后重新导入）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (current.duplicateGroups.isEmpty() && current.missingBooks.isEmpty() && current.corruptBooks.isEmpty()) {
        Text(
            text = "书库状态良好：没有重复书名、缺失或损坏的条目",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        return
    }
    current.duplicateGroups.forEach { group ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = "重复书名：《${group.first().title}》× ${group.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { mergeGroup = group }) { Text("合并") }
        }
    }
    if (current.missingBooks.isNotEmpty()) {
        Text(
            text = "文件缺失：${current.missingBooks.size} 本 — " +
                current.missingBooks.joinToString("、") { it.title },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        // P1-4 批量修复: a rescan rebuilds both shelves from disk, which drops entries whose
        // files vanished; per-book deletion stays the tool for duplicate titles.
        TextButton(
            onClick = { confirmRescan = true },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) { Text("重新扫描并清理缺失条目") }
        rescanStatus?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
    mergeGroup?.let { group ->
        MergeDuplicatesDialog(
            group = group,
            onDismiss = { mergeGroup = null },
            onConfirm = { keep ->
                mergeGroup = null
                scope.launch {
                    runCatching {
                        group.filter { it.location != keep.location }.forEach { duplicate ->
                            if (duplicate.mediaType == app.miyomu.model.MediaType.NOVEL) {
                                graph.novels.delete(duplicate)
                            } else {
                                graph.comics.delete(duplicate)
                            }
                        }
                        graph.library.refresh(graph.root)
                        graph.novelLibrary.refresh(graph.root)
                    }
                }
            },
        )
    }
    // P1-6 OPDS 导出: catalog.xml + book copies under <libraryRoot>/.opds-export (dot-prefixed,
    // so the scanner ignores the export). Point any OPDS reader at the folder to browse it.
    TextButton(
        onClick = { confirmOpdsExport = true },
        modifier = Modifier.padding(horizontal = 16.dp),
    ) { Text("导出 OPDS 目录") }
    TextButton(
        onClick = { confirmOpdsImport = true },
        modifier = Modifier.padding(horizontal = 16.dp),
    ) { Text("从 .opds-export 导入") }
    opdsStatus?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (opdsServer == null) {
            TextButton(onClick = {
                scope.launch {
                    val handle = runCatching {
                        startOpdsServer(
                            port = 8080,
                            exportRoot = java.io.File(graph.root.location, OpdsCatalog.EXPORT_DIR_NAME),
                            title = "Miyomu Library",
                            booksProvider = { comics + novels },
                            nowMillis = graph.nowMillis,
                        )
                    }.getOrNull()
                    opdsServer = handle
                    opdsStatus = if (handle != null) {
                        val host = lanAddress() ?: "localhost"
                        "OPDS 服务已启动：http://$host:${handle.port}/catalog.xml"
                    } else {
                        "OPDS 服务启动失败（端口被占用？）"
                    }
                }
            }) { Text("启动 OPDS 服务（局域网）") }
        } else {
            TextButton(onClick = {
                opdsServer?.stop()
                opdsServer = null
                opdsStatus = "OPDS 服务已停止"
            }) { Text("停止 OPDS 服务") }
        }
    }
    TextButton(
        onClick = { showRemoteImport = true },
        modifier = Modifier.padding(horizontal = 16.dp),
    ) { Text("从 OPDS feed 导入") }
    if (showRemoteImport) {
        RemoteFeedImportDialog(
            graph = graph,
            onDismiss = { showRemoteImport = false },
        )
    }
    if (confirmOpdsImport) {
        AlertDialog(
            onDismissRequest = { confirmOpdsImport = false },
            title = { Text("导入 OPDS 目录") },
            text = { Text("将书库根目录 .opds-export/books/ 下的书目复制回书架并重新扫描（已存在的文件不会覆盖）。继续？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpdsImport = false
                    scope.launch {
                        opdsStatus = withContext(Dispatchers.IO) {
                            runCatching {
                                val exportRoot = java.io.File(graph.root.location, OpdsCatalog.EXPORT_DIR_NAME)
                                val imported = OpdsCatalog.import(exportRoot, java.io.File(graph.root.location))
                                graph.library.refresh(graph.root)
                                graph.novelLibrary.refresh(graph.root)
                                "已导入 $imported 本"
                            }.getOrElse { "导入失败：${it.message}" }
                        }
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpdsImport = false }) { Text("取消") }
            },
        )
    }
    if (confirmOpdsExport) {
        AlertDialog(
            onDismissRequest = { confirmOpdsExport = false },
            title = { Text("导出 OPDS 目录") },
            text = { Text("将在书库根目录的 .opds-export/ 下生成 catalog.xml 并复制全部书目（漫画与小说），供 OPDS 阅读器浏览。继续？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmOpdsExport = false
                    scope.launch {
                        opdsStatus = withContext(Dispatchers.IO) {
                            runCatching {
                                val exportRoot = java.io.File(graph.root.location, OpdsCatalog.EXPORT_DIR_NAME)
                                val done = OpdsCatalog.export(
                                    exportRoot,
                                    "Miyomu Library",
                                    comics + novels,
                                    graph.nowMillis(),
                                )
                                "已导出到 ${done.absolutePath}"
                            }.getOrElse { "导出失败：${it.message}" }
                        }
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { confirmOpdsExport = false }) { Text("取消") }
            },
        )
    }
    if (confirmRescan) {
        AlertDialog(
            onDismissRequest = { confirmRescan = false },
            title = { Text("重新扫描书库") },
            text = { Text("将从书库根目录重新扫描双书架，文件已删除的条目会被移除。继续？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRescan = false
                    scope.launch {
                        runCatching {
                            graph.library.refresh(graph.root)
                            graph.novelLibrary.refresh(graph.root)
                        }.onSuccess { rescanStatus = "重新扫描完成" }
                            .onFailure { rescanStatus = "重新扫描失败：${it.message}" }
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRescan = false }) { Text("取消") }
            },
        )
    }
}


/**
 * P1-4 重复项合并: pick which copy of a duplicate-title group to keep; the rest are deleted
 * from disk and the rescan drops them from the shelves.
 */
@Composable
private fun MergeDuplicatesDialog(
    group: List<app.miyomu.local.LocalBook>,
    onDismiss: () -> Unit,
    onConfirm: (keep: app.miyomu.local.LocalBook) -> Unit,
) {
    var keepLocation by remember { mutableStateOf(group.first().location) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("合并重复：《${group.first().title}》") },
        text = {
            Column {
                Text(
                    text = "选择要保留的一本，其余副本将从磁盘删除：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                group.forEach { book ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { keepLocation = book.location }
                            .padding(vertical = 6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = keepLocation == book.location,
                            onClick = { keepLocation = book.location },
                        )
                        Column {
                            Text(
                                text = book.location.substringAfterLast('/'),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                            Text(
                                text = book.mediaType.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                group.firstOrNull { it.location == keepLocation }?.let(onConfirm)
            }) { Text("删除其余副本") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/**
 * P1-6 外部 feed 接入: fetch a remote OPDS catalog, list its acquisition entries and
 * download them into the library root; the rescan shelves everything.
 */
@Composable
private fun RemoteFeedImportDialog(graph: MiyomuGraph, onDismiss: () -> Unit) {
    var feedUrl by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<app.miyomu.local.OpdsRemoteBook>>(emptyList()) }
    var downloaded by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val client = remember { io.ktor.client.HttpClient() }
    androidx.compose.runtime.DisposableEffect(client) {
        onDispose { client.close() }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("从 OPDS feed 导入") },
        text = {
            Column {
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = { feedUrl = it },
                    label = { Text("Feed URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            entries = withContext(Dispatchers.IO) {
                                runCatching { app.miyomu.local.fetchOpdsFeed(client, feedUrl.trim()) }
                                    .getOrElse {
                                        message = "获取失败：${it.message}"
                                        emptyList()
                                    }
                            }
                            busy = false
                        }
                    }
                ) { Text(if (busy) "获取中…" else "获取条目") }
                if (entries.isNotEmpty()) {
                    Text(
                        text = "共 ${entries.size} 条，已下载 $downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = {
                        scope.launch {
                            busy = true
                            val count = withContext(Dispatchers.IO) {
                                var done = 0
                                entries.forEach { entry ->
                                    runCatching {
                                        app.miyomu.local.downloadOpdsBook(
                                            client,
                                            entry,
                                            java.io.File(graph.root.location),
                                        )
                                    }.onSuccess { done += 1 }
                                }
                                done
                            }
                            runCatching {
                                graph.library.refresh(graph.root)
                                graph.novelLibrary.refresh(graph.root)
                            }
                            downloaded = count
                            busy = false
                            message = "已下载 $count 本并重新扫描书架"
                        }
                    }) { Text(if (busy) "下载中…" else "下载全部") }
                }
                message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}