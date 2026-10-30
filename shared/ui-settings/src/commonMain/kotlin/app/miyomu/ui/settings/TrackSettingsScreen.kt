@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.miyomu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.track.AniListClient
import app.miyomu.track.BangumiClient
import app.miyomu.track.TrackException
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * P1-9 追踪（默认关闭）: token-based AniList / Bangumi accounts with a viewer check. Per-item
 * binding and progress push build on the `shared/track` clients; tokens live in the app's
 * preference store, and nothing is contacted while the toggle is off.
 */
@Composable
fun TrackSettingsRoute(graph: MiyomuGraph) {
    val navigator = LocalMiyomuNavigator.current
    val scope = rememberCoroutineScope()
    val client = remember { HttpClient() }
    DisposableEffect(client) {
        onDispose { client.close() }
    }

    val enabledPref = remember { graph.preferenceStore.getBoolean("track_enabled", false) }
    val enabled by enabledPref.changes().collectAsState(initial = enabledPref.get())
    val bindingStore = remember {
        app.miyomu.track.TrackBindingStore(
            read = { graph.preferenceStore.getString("track_bindings_v1", "").get() },
            write = { graph.preferenceStore.getString("track_bindings_v1", "").set(it) },
        )
    }
    var bindings by remember { mutableStateOf(bindingStore.all()) }
    var anilistToken by remember { mutableStateOf(graph.preferenceStore.getString("track_anilist_token", "").get()) }
    var bangumiToken by remember { mutableStateOf(graph.preferenceStore.getString("track_bangumi_token", "").get()) }
    var status by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("追踪") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
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
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("启用追踪", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "默认关闭；开启后才会携带令牌访问 AniList / Bangumi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabledPref.set(it) },
                )
            }

            TokenSection(
                title = "AniList",
                hint = "在 anilist.co 账户设置 → API 中生成个人令牌",
                token = anilistToken,
                onTokenChange = {
                    anilistToken = it
                    graph.preferenceStore.getString("track_anilist_token", "").set(it)
                },
                onVerify = {
                    if (!enabled) {
                        status = "追踪未启用"
                    } else {
                        scope.launch {
                            status = runCatching {
                                withContext(Dispatchers.IO) {
                                    AniListClient(client).viewer(anilistToken.trim())
                                }
                            }.fold(
                                onSuccess = { "AniList 已连接：${it.name}" },
                                onFailure = { failure ->
                                    (failure as? TrackException)?.message
                                        ?: "验证失败：${failure.message}"
                                },
                            )
                        }
                    }
                },
            )

            TokenSection(
                title = "Bangumi",
                hint = "在 bgm.tv 接入令牌页面创建访问令牌",
                token = bangumiToken,
                onTokenChange = {
                    bangumiToken = it
                    graph.preferenceStore.getString("track_bangumi_token", "").set(it)
                },
                onVerify = {
                    if (!enabled) {
                        status = "追踪未启用"
                    } else {
                        scope.launch {
                            status = runCatching {
                                withContext(Dispatchers.IO) {
                                    BangumiClient(client).viewer(bangumiToken.trim())
                                }
                            }.fold(
                                onSuccess = { "Bangumi 已连接：${it.name}" },
                                onFailure = { failure ->
                                    (failure as? TrackException)?.message
                                        ?: "验证失败：${failure.message}"
                                },
                            )
                        }
                    }
                },
            )

            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // P1-9 绑定管理列表: every manual binding with one-tap removal.
            Text(
                text = "已绑定条目（${bindings.size}）",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (bindings.isEmpty()) {
                Text(
                    text = "还没有绑定；在书目的详情页点击追踪图标即可绑定。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            bindings.forEach { binding ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = binding.remoteTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        Text(
                            text = "${binding.target.name} · 进度 ${binding.progress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = {
                        bindingStore.remove(binding.itemId)
                        bindings = bindingStore.all()
                    }) { Text("解绑") }
                }
            }
        }
    }
}

@Composable
private fun TokenSection(
    title: String,
    hint: String,
    token: String,
    onTokenChange: (String) -> Unit,
    onVerify: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            label = { Text("访问令牌") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        TextButton(onClick = onVerify) { Text("验证连接") }
    }
}
