package app.miyomu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.danmaku.DanmakuCredentials
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import kotlinx.coroutines.launch

/**
 * Danmaku settings: dandanplay open-API credentials (user-registered at dev.dandanplay.com) and
 * the default on/off state. Danmaku stays off until valid credentials are saved.
 */
@Composable
fun DanmakuSettingsRoute(
    graph: MiyomuGraph,) {
        val navigator = LocalMiyomuNavigator.current
        DanmakuSettingsContent(graph = graph, onBack = { navigator.pop() })
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DanmakuSettingsContent(
    graph: MiyomuGraph,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var appId by rememberSaveable { mutableStateOf(graph.preferenceStore.getString(graph.danmakuKeys.APP_ID).get()) }
    var secret by rememberSaveable { mutableStateOf(graph.preferenceStore.getString(graph.danmakuKeys.SECRET).get()) }
    var enabled by remember { mutableStateOf(graph.preferenceStore.getBoolean(graph.danmakuKeys.ENABLED, true).get()) }
    var status by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("弹幕设置") },
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
            Text(
                text = "弹幕由弹弹play 开放 API 提供。请在 dev.dandanplay.com 注册应用后填入 AppId / AppSecret；" +
                    "未配置时播放器不会请求弹幕。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = appId,
                onValueChange = { appId = it },
                label = { Text("AppId") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("AppSecret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("默认开启弹幕", modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        graph.preferenceStore.getBoolean(graph.danmakuKeys.ENABLED, true).set(it)
                    },
                )
            }
            Button(
                onClick = {
                    if (appId.isBlank() || secret.isBlank()) {
                        status = "AppId 与 AppSecret 不能为空"
                        return@Button
                    }
                    scope.launch {
                        runCatching {
                            graph.danmaku.setCredentials(DanmakuCredentials(appId.trim(), secret.trim()))
                            graph.preferenceStore.getString(graph.danmakuKeys.APP_ID).set(appId.trim())
                            graph.preferenceStore.getString(graph.danmakuKeys.SECRET).set(secret.trim())
                        }.onSuccess { status = "已保存" }
                            .onFailure { status = "保存失败：${it.message}" }
                    }
                },
            ) { Text("保存") }
            status?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
