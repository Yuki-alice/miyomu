package app.miyomu.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.data.BackupManager
import app.miyomu.network.WebDavClient
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import java.io.File

/** Backup / restore to a local file, plus WebDAV upload & download (C3c). */
class BackupSyncRoute(
    private val graph: MiyomuGraph,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        BackupSyncContent(graph = graph, onBack = { navigator.pop() })
    }
}

private const val WebDavUrlKey = "webdav_url"
private const val WebDavUserKey = "webdav_user"
private const val WebDavPassKey = "webdav_pass"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncContent(
    graph: MiyomuGraph,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val manager = remember(graph) { BackupManager(graph.data) }

    var localFile by remember { mutableStateOf(graph.backupFile) }
    var status by remember { mutableStateOf<String?>(null) }

    var webDavUrl by rememberSaveable { mutableStateOf(graph.preferenceStore.getString(WebDavUrlKey).get()) }
    var webDavUser by rememberSaveable { mutableStateOf(graph.preferenceStore.getString(WebDavUserKey).get()) }
    var webDavPass by rememberSaveable { mutableStateOf(graph.preferenceStore.getString(WebDavPassKey).get()) }

    fun webDavReady(): Boolean {
        if (webDavUrl.isBlank()) {
            status = "请先填写 WebDAV 文件 URL"
            return false
        }
        graph.preferenceStore.getString(WebDavUrlKey).set(webDavUrl.trim())
        graph.preferenceStore.getString(WebDavUserKey).set(webDavUser)
        graph.preferenceStore.getString(WebDavPassKey).set(webDavPass)
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("备份与同步") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("本地备份", style = MaterialTheme.typography.titleMedium)
            Text(
                text = localFile?.absolutePath ?: "此平台未配置备份目录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val file = localFile
                        if (file == null) {
                            status = "此平台未配置备份目录"
                            return@Button
                        }
                        scope.launch {
                            runCatching {
                                val json = manager.export(graph.nowMillis())
                                manager.writeToFile(json, file)
                            }.onSuccess { status = "已备份到 ${file.absolutePath}" }
                                .onFailure { status = "备份失败：${it.message}" }
                        }
                    },
                ) { Text("备份到本地") }
                OutlinedButton(
                    onClick = {
                        val file = localFile
                        if (file == null || !file.isFile) {
                            status = "本地备份文件不存在"
                            return@OutlinedButton
                        }
                        scope.launch {
                            runCatching { manager.restore(manager.readFromFile(file), replace = true) }
                                .onSuccess { result ->
                                    status = "已恢复：书库 ${result.library}，进度 ${result.progress}，" +
                                        "章节 ${result.chapters}，剧集 ${result.episodes}"
                                }
                                .onFailure { status = "恢复失败：${it.message}" }
                        }
                    },
                ) { Text("从本地恢复") }
            }

            HorizontalDivider()

            Text("WebDAV 同步", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = webDavUrl,
                onValueChange = { webDavUrl = it },
                label = { Text("备份文件 URL（如 https://dav.example.com/miyomu/backup.json）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = webDavUser,
                    onValueChange = { webDavUser = it },
                    label = { Text("用户名") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = webDavPass,
                    onValueChange = { webDavPass = it },
                    label = { Text("密码") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (!webDavReady()) return@Button
                        scope.launch {
                            runCatching {
                                val json = manager.export(graph.nowMillis())
                                WebDavClient().upload(webDavUrl.trim(), webDavUser, webDavPass, json.encodeToByteArray())
                            }.onSuccess { status = "已上传到 WebDAV" }
                                .onFailure { status = "上传失败：${it.message}" }
                        }
                    },
                ) { Text("上传备份") }
                OutlinedButton(
                    onClick = {
                        if (!webDavReady()) return@OutlinedButton
                        scope.launch {
                            runCatching {
                                val bytes = WebDavClient().download(webDavUrl.trim(), webDavUser, webDavPass)
                                manager.restore(bytes.decodeToString(), replace = true)
                            }.onSuccess { result ->
                                status = "已从 WebDAV 恢复：书库 ${result.library}，进度 ${result.progress}"
                            }.onFailure { status = "下载/恢复失败：${it.message}" }
                        }
                    },
                ) { Text("从 WebDAV 恢复") }
            }

            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Backup file location is chosen by the platform; null disables local backup rows. */
internal fun defaultBackupFile(homeDir: File?): File? =
    homeDir?.let { File(it, "backup.json") }
