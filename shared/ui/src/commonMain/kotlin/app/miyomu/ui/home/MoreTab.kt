package app.miyomu.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.ui.MiyomuGraph
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

class MoreTab(
    private val graph: MiyomuGraph,
    private val onOpenSources: () -> Unit,
    private val onOpenDownloads: (() -> Unit)? = null,
    private val onOpenBackup: (() -> Unit)? = null,
    private val onOpenDanmaku: (() -> Unit)? = null,
    private val onOpenSettings: () -> Unit = {},
) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 4u, title = "更多")

    @Composable
    override fun Content() {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "更多",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            MoreRow(icon = Icons.Outlined.FolderOpen, title = "本地库", subtitle = graph.root.location)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreRow(
                icon = Icons.Outlined.Download,
                title = "下载队列",
                subtitle = "离线章节的进度与管理",
                onClick = onOpenDownloads,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreRow(
                icon = Icons.Outlined.Extension,
                title = "源仓库",
                subtitle = "添加索引 JSON，安装 JS 源",
                onClick = onOpenSources,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreRow(
                icon = Icons.Outlined.Settings,
                title = "备份与同步",
                subtitle = "本地备份与 WebDAV 同步",
                onClick = onOpenBackup,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreRow(
                icon = Icons.Outlined.Settings,
                title = "弹幕设置",
                subtitle = "弹弹play 凭据与默认开关",
                onClick = onOpenDanmaku,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreRow(
                icon = Icons.Outlined.Settings,
                title = "设置",
                subtitle = "阅读、备份、弹幕",
                onClick = onOpenSettings,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MoreRow(icon = Icons.Outlined.Info, title = "关于 Miyomu", subtitle = "0.1.0 · Apache-2.0")
        }
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
