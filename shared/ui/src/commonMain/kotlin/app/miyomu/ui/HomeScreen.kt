package app.miyomu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalBook
import app.miyomu.local.LocalComicRepository
import app.miyomu.model.LibraryItem

private enum class HomeTab(val label: String) {
    LIBRARY("书架"),
    UPDATES("更新"),
    HISTORY("历史"),
    BROWSE("浏览"),
    MORE("更多"),
}

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.LIBRARY -> Icons.AutoMirrored.Outlined.MenuBook
    HomeTab.UPDATES -> Icons.Outlined.Update
    HomeTab.HISTORY -> Icons.Outlined.History
    HomeTab.BROWSE -> Icons.Outlined.Explore
    HomeTab.MORE -> Icons.Outlined.MoreHoriz
}

@Composable
fun HomeScreen(
    books: List<LocalBook>,
    comics: LocalComicRepository,
    data: MiyomuData,
    libraryPath: String,
    onOpenBook: (LocalBook) -> Unit,
    onRefresh: () -> Unit,
    onOpenLibraryFolder: (() -> Unit)?,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.LIBRARY) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp

        if (wide) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Text(
                            text = "Miyomu",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    },
                ) {
                    HomeTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                TabIcon(tab = tab, count = books.size, selected = selectedTab == tab)
                            },
                            label = { Text(tab.label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    TabContent(
                        tab = selectedTab,
                        books = books,
                        comics = comics,
                        data = data,
                        libraryPath = libraryPath,
                        onOpenBook = onOpenBook,
                        onRefresh = onRefresh,
                        onOpenLibraryFolder = onOpenLibraryFolder,
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        HomeTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    TabIcon(tab = tab, count = books.size, selected = selectedTab == tab)
                                },
                                label = { Text(tab.label) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    TabContent(
                        tab = selectedTab,
                        books = books,
                        comics = comics,
                        data = data,
                        libraryPath = libraryPath,
                        onOpenBook = onOpenBook,
                        onRefresh = onRefresh,
                        onOpenLibraryFolder = onOpenLibraryFolder,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: HomeTab, count: Int, selected: Boolean) {
    if (tab == HomeTab.LIBRARY && count > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(count.toString())
                }
            },
        ) {
            Icon(tab.icon(), contentDescription = tab.label)
        }
    } else {
        Icon(tab.icon(), contentDescription = tab.label)
    }
}

@Composable
private fun TabContent(
    tab: HomeTab,
    books: List<LocalBook>,
    comics: LocalComicRepository,
    data: MiyomuData,
    libraryPath: String,
    onOpenBook: (LocalBook) -> Unit,
    onRefresh: () -> Unit,
    onOpenLibraryFolder: (() -> Unit)?,
) {
    when (tab) {
        HomeTab.LIBRARY -> LibraryScreen(
            books = books,
            libraryPath = libraryPath,
            comics = comics,
            onOpen = onOpenBook,
            onRefresh = onRefresh,
            onOpenLibraryFolder = onOpenLibraryFolder,
        )
        HomeTab.UPDATES -> EmptyTab(
            icon = Icons.Outlined.Update,
            title = "暂无更新",
            body = "在线源与更新检查会在 M2 接入；当前从书架打开本地漫画。",
        )
        HomeTab.HISTORY -> HistoryScreen(
            data = data,
            books = books,
            comics = comics,
            onOpenBook = onOpenBook,
        )
        HomeTab.BROWSE -> BrowseScreen(
            libraryPath = libraryPath,
            onRefresh = onRefresh,
            onOpenLibraryFolder = onOpenLibraryFolder,
        )
        HomeTab.MORE -> MoreScreen(libraryPath = libraryPath)
    }
}

@Composable
private fun EmptyTab(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp).height(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryScreen(
    data: MiyomuData,
    books: List<LocalBook>,
    comics: LocalComicRepository,
    onOpenBook: (LocalBook) -> Unit,
) {
    val history by data.library.observeLibrary().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "历史",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (history.isEmpty()) {
            EmptyTab(
                icon = Icons.Outlined.History,
                title = "还没有阅读记录",
                body = "打开一本书后，最近阅读会出现在这里。",
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = history, key = { it.id }) { item ->
                    val book = books.firstOrNull { it.id == item.sourceKey }
                    HistoryRow(
                        item = item,
                        book = book,
                        comics = comics,
                        onClick = { book?.let(onOpenBook) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: LibraryItem,
    book: LocalBook?,
    comics: LocalComicRepository,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(44.dp).height(60.dp)) {
            if (book != null) {
                BookCover(
                    book = book,
                    comics = comics,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item.title.take(1), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "本地漫画",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick, enabled = book != null) {
            Text("继续")
        }
    }
}

@Composable
private fun BrowseScreen(
    libraryPath: String,
    onRefresh: () -> Unit,
    onOpenLibraryFolder: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(56.dp).height(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("本地导入", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "把漫画目录或 CBZ/ZIP 放进：\n$libraryPath",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onOpenLibraryFolder != null) {
                Button(onClick = onOpenLibraryFolder) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("打开目录")
                }
            }
            TextButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("刷新书架")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "在线源浏览与源仓库在 M2 接入。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoreScreen(libraryPath: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "更多",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        MoreRow(icon = Icons.Outlined.FolderOpen, title = "本地库", subtitle = libraryPath)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        MoreRow(icon = Icons.Outlined.Settings, title = "阅读设置", subtitle = "阅读模式、方向与排版（后续）")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        MoreRow(icon = Icons.Outlined.Info, title = "关于 Miyomu", subtitle = "0.1.0 · Apache-2.0")
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
