package app.miyomu.ui.track

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.miyomu.track.AniListClient
import app.miyomu.track.BangumiClient
import app.miyomu.track.TrackBinding
import app.miyomu.track.TrackBindingStore
import app.miyomu.track.TrackException
import app.miyomu.track.TrackSearchResult
import app.miyomu.track.TrackTarget
import app.miyomu.ui.MiyomuGraph
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * P1-9 逐书绑定与进度推送: pick a tracker series for one library item (manual search), then
 * push the locally read-chapter count as progress. The dialog is self-contained — preferences,
 * clients and their lifetimes come from [graph]; bindings persist through [TrackBindingStore].
 */
@Composable
fun TrackBindingDialog(
    graph: MiyomuGraph,
    itemId: String,
    initialTitle: String,
    currentProgress: Int,
    onDismiss: () -> Unit,
) {
    val enabled = remember { graph.preferenceStore.getBoolean("track_enabled", false).get() }
    val store = remember {
        TrackBindingStore(
            read = { graph.preferenceStore.getString("track_bindings_v1", "").get() },
            write = { graph.preferenceStore.getString("track_bindings_v1", "").set(it) },
        )
    }
    val client = remember { HttpClient() }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    val scope = rememberCoroutineScope()

    var binding by remember { mutableStateOf(store.bindingFor(itemId)) }
    var target by remember { mutableStateOf(TrackTarget.ANILIST) }
    var query by remember { mutableStateOf(initialTitle) }
    var results by remember { mutableStateOf<List<TrackSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun search() {
        if (!enabled) {
            message = "追踪未启用：请先在“追踪”设置中开启并填写令牌"
            return
        }
        scope.launch {
            searching = true
            message = null
            val anilistToken = graph.preferenceStore.getString("track_anilist_token", "").get()
            val bangumiToken = graph.preferenceStore.getString("track_bangumi_token", "").get()
            runCatching {
                withContext(Dispatchers.IO) {
                    when (target) {
                        TrackTarget.ANILIST -> AniListClient(client).search(anilistToken, query)
                        TrackTarget.BANGUMI -> BangumiClient(client).searchBooks(bangumiToken, query)
                    }
                }
            }.fold(
                onSuccess = { found -> results = found },
                onFailure = { failure ->
                    message = (failure as? TrackException)?.message ?: "搜索失败：${failure.message}"
                },
            )
            searching = false
        }
    }

    fun push() {
        val current = binding ?: return
        scope.launch {
            message = pushProgress(graph, client, current, currentProgress)
            if (message == null) binding = store.bindingFor(itemId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("追踪绑定") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val current = binding
                if (current != null) {
                    Text(
                        text = "已绑定：${current.remoteTitle}（${current.target.name} · 进度 ${current.progress}）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "本地已读 $currentProgress 章",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        TextButton(onClick = ::push) { Text("推送进度 $currentProgress") }
                        TextButton(onClick = {
                            store.remove(itemId)
                            binding = null
                            message = null
                        }) { Text("解除绑定") }
                    }
                } else {
                    Text(
                        text = "选择追踪站点并搜索对应条目；绑定后可推送本地已读章节数。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = target == TrackTarget.ANILIST,
                            onClick = { target = TrackTarget.ANILIST },
                            label = { Text("AniList") },
                        )
                        FilterChip(
                            selected = target == TrackTarget.BANGUMI,
                            onClick = { target = TrackTarget.BANGUMI },
                            label = { Text("Bangumi") },
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("条目名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                    TextButton(onClick = ::search) {
                        Text(if (searching) "搜索中…" else "搜索")
                    }
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    }
                    if (results.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                            items(results.size) { index ->
                                val result = results[index]
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val created = TrackBinding(
                                                itemId = itemId,
                                                target = target,
                                                remoteId = result.id,
                                                remoteTitle = result.title,
                                                progress = currentProgress,
                                            )
                                            store.upsert(created)
                                            binding = created
                                            results = emptyList()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                )
                            }
                        }
                    }
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

/** Pushes the local progress to the bound tracker; null on success, error text otherwise. */
private suspend fun pushProgress(
    graph: MiyomuGraph,
    client: HttpClient,
    binding: TrackBinding,
    progress: Int,
): String? {
    val enabled = graph.preferenceStore.getBoolean("track_enabled", false).get()
    if (!enabled) return "追踪未启用"
    val anilistToken = graph.preferenceStore.getString("track_anilist_token", "").get()
    val bangumiToken = graph.preferenceStore.getString("track_bangumi_token", "").get()
    val store = TrackBindingStore(
        read = { graph.preferenceStore.getString("track_bindings_v1", "").get() },
        write = { graph.preferenceStore.getString("track_bindings_v1", "").set(it) },
    )
    return withContext(Dispatchers.IO) {
        runCatching {
            when (binding.target) {
                TrackTarget.ANILIST ->
                    AniListClient(client).updateProgress(anilistToken, binding.remoteId, progress)
                TrackTarget.BANGUMI ->
                    BangumiClient(client).updateBookProgress(bangumiToken, binding.remoteId, progress)
            }
            store.upsert(binding.copy(progress = progress))
        }.fold(
            onSuccess = { null },
            onFailure = { failure ->
                (failure as? TrackException)?.message ?: "推送失败：${failure.message}"
            },
        )
    }
}
