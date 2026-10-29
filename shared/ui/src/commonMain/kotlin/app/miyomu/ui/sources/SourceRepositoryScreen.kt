package app.miyomu.ui.sources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.source.repo.AvailableSource
import app.miyomu.source.repo.InstalledSource
import app.miyomu.source.repo.SourceCatalog
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch

/**
 * Manages user-added JS source repositories: repository URLs, available packages and the
 * locally installed scripts. Loading installed sources into the runtime comes next (Phase C).
 */
class SourceRepositoryScreen(private val graph: MiyomuGraph) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        SourceRepositoryContent(graph = graph, onBack = { navigator.pop() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceRepositoryContent(graph: MiyomuGraph, onBack: () -> Unit) {
    val manager = graph.sourceInstalls
    val scope = rememberCoroutineScope()
    val installed by manager.installed.collectAsState()
    val repositories by manager.repositories.collectAsState()

    var catalog by remember { mutableStateOf(SourceCatalog()) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addUrl by remember { mutableStateOf("") }

    fun refresh() {
        if (loading) return
        loading = true
        scope.launch {
            runCatching { manager.fetchCatalog() }
                .onSuccess {
                    catalog = it
                    error = it.errors.firstOrNull()
                }
                .onFailure { error = it.message }
            loading = false
        }
    }

    fun updateAvailableFor(source: InstalledSource): AvailableSource? =
        catalog.available.firstOrNull { it.packageInfo.id == source.id && it.updateAvailable }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            runCatching { action() }
                .onSuccess { refresh() }
                .onFailure { error = it.message ?: it::class.simpleName }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        manager.ensureLoaded()
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("源仓库") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = ::refresh, enabled = !loading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading || busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { SectionTitle("已安装（${installed.size}）") }
                if (installed.isEmpty()) {
                    item { Hint("还没有安装源。添加仓库地址后从下方列表安装。") }
                }
                items(installed, key = { it.id }) { source ->
                    val update = updateAvailableFor(source)
                    InstalledRow(
                        source = source,
                        update = update,
                        onUpdate = update?.let { target -> { run { manager.install(target) } } },
                        onUninstall = { run { manager.uninstall(source.id) } },
                    )
                }

                val installable = catalog.available.filterNot { it.isInstalled }
                item { SectionTitle("可安装（${installable.size}）") }
                if (installable.isEmpty()) {
                    item { Hint("仓库为空或尚未刷新。") }
                }
                items(installable, key = { "${it.repositoryUrl}#${it.packageInfo.id}" }) { available ->
                    AvailableRow(
                        available = available,
                        onInstall = { run { manager.install(available) } },
                    )
                }

                item { SectionTitle("仓库") }
                if (repositories.isEmpty()) {
                    item { Hint("添加一个索引 JSON 的 URL 开始。") }
                }
                items(repositories, key = { it }) { repository ->
                    RepositoryRow(
                        url = repository,
                        onRemove = { run { manager.removeRepository(repository) } },
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("添加仓库", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                error?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                addUrl = ""
            },
            title = { Text("添加源仓库") },
            text = {
                Column {
                    Text(
                        text = "填写索引 JSON 的 URL。Miyomu 不内置、不推荐任何仓库。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addUrl,
                        onValueChange = { addUrl = it },
                        singleLine = true,
                        label = { Text("https://example.com/index.json") },
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        addUrl = ""
                    },
                ) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = addUrl.trim()
                        showAddDialog = false
                        addUrl = ""
                        run { manager.addRepository(url) }
                    },
                    enabled = addUrl.isNotBlank(),
                ) { Text("添加") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun InstalledRow(
    source: InstalledSource,
    update: AvailableSource?,
    onUpdate: (() -> Unit)?,
    onUninstall: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(source.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = listOfNotNull(
                        "v${source.version}",
                        source.lang.takeIf { it.isNotBlank() },
                        source.description?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onUpdate != null) {
                TextButton(onClick = onUpdate) { Text("更新到 ${update?.packageInfo?.version}") }
            }
            IconButton(onClick = onUninstall) {
                Icon(Icons.Outlined.Delete, contentDescription = "卸载")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun AvailableRow(available: AvailableSource, onInstall: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(available.packageInfo.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = listOfNotNull(
                    "v${available.packageInfo.version}",
                    available.packageInfo.lang.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onInstall) { Text("安装") }
    }
}

@Composable
private fun RepositoryRow(url: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "移除仓库")
        }
    }
}
