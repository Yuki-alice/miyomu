package app.miyomu.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import app.miyomu.ui.MiyomuGraph

class UpdatesTab(private val graph: MiyomuGraph) : Tab {

    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "更新")

    @Composable
    override fun Content() {
        EmptyTab(
            icon = Icons.Outlined.Update,
            title = "暂无更新",
            body = "在线源与更新检查后续接入；当前从书架打开本地漫画。",
        )
    }
}
