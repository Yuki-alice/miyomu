package app.miyomu.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.miyomu.local.LocalBook
import app.miyomu.model.MediaType
import app.miyomu.source.manager.SourceRuntimeInfo
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.history.MangaHistoryTab
import app.miyomu.ui.navigation.BackupKey
import app.miyomu.ui.navigation.DanmakuSettingsKey
import app.miyomu.ui.navigation.DownloadsKey
import app.miyomu.ui.navigation.GlobalSearchKey
import app.miyomu.ui.navigation.LocalMiyomuNavigator
import app.miyomu.ui.navigation.MangaDetailKey
import app.miyomu.ui.navigation.MangaReaderKey
import app.miyomu.ui.navigation.MiyomuKey
import app.miyomu.ui.navigation.NovelDetailKey
import app.miyomu.ui.navigation.SettingsKey
import app.miyomu.ui.navigation.ReadingStatsKey
import app.miyomu.ui.navigation.TrackSettingsKey
import app.miyomu.ui.navigation.SourceDetailKey
import app.miyomu.ui.navigation.SourceRepositoryKey
import app.miyomu.ui.navigation.SourceSearchKey
import app.miyomu.ui.navigation.VideoDetailKey
import app.miyomu.ui.updates.UpdatesTab
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The tab shell.
 *
 * Tabs are local state, not back stack entries: the stack only holds destinations the user can pop
 * out of, so switching tabs never grows history. Anything a tab opens (detail, reader, player) is
 * pushed onto the single root stack, which is what makes "back" predictable and lets a reader be
 * restored from a key alone.
 */
@Composable
fun HomeScreen(graph: MiyomuGraph) {
    val navigator = LocalMiyomuNavigator.current
    val books by graph.library.books.collectAsState()
    val novelBooks by graph.novelLibrary.books.collectAsState()

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = HomeTab.entries
    val selected = tabs[selectedIndex.coerceIn(0, tabs.lastIndex)]

    val onOpenBook: (LocalBook) -> Unit = remember(navigator) {
        { book ->
            navigator.push(
                when (book.mediaType) {
                    MediaType.NOVEL -> NovelDetailKey(book.id)
                    MediaType.VIDEO -> VideoDetailKey(book.id)
                    else -> MangaDetailKey(book.id)
                },
            )
        }
    }

    // History rows pointing at an installed source resolve the item off the main thread: the
    // query hits Room through the data port and must never block the click handler.
    val scope = rememberCoroutineScope()
    val onOpenSourceItem: (String, String) -> Unit = { scopeId, sourceKey ->
        scope.launch {
            val item = runCatching { graph.data.library.get("$scopeId:$sourceKey") }.getOrNull()
            if (item != null) {
                navigator.push(
                    SourceDetailKey(scopeId, scopeId, sourceKey, item.title, item.coverUrl),
                )
            }
        }
    }

    // History rows point at either a local book or an item that lives in an installed source.
    val onHistoryResume: (String, String) -> Unit = remember(navigator, onOpenSourceItem) {
        { itemId, _ ->
            val scopeId = itemId.substringBefore(':')
            val sourceKey = itemId.substringAfter(':')
            if (scopeId == "local") {
                graph.library.books.value.firstOrNull { it.id == sourceKey }?.let {
                    navigator.push(MangaReaderKey(it.id, 0, 0))
                }
            } else {
                onOpenSourceItem(scopeId, sourceKey)
            }
        }
    }
    val onHistoryOpenDetails: (String) -> Unit = remember(navigator, onOpenBook, onOpenSourceItem) {
        { itemId ->
            val scopeId = itemId.substringBefore(':')
            val sourceKey = itemId.substringAfter(':')
            if (scopeId == "local") {
                graph.library.books.value.firstOrNull { it.id == sourceKey }?.let(onOpenBook)
            } else {
                onOpenSourceItem(scopeId, sourceKey)
            }
        }
    }

    val onOpenSources: () -> Unit = remember(navigator) { { navigator.push(SourceRepositoryKey) } }
    val onOpenDownloads: (() -> Unit)? = graph.downloadQueue?.let { { navigator.push(DownloadsKey) } }
    val onOpenBackup: (() -> Unit)? = graph.backupFile?.let { { navigator.push(BackupKey) } }
    val onOpenGlobalSearch: () -> Unit = remember(navigator) { { navigator.push(GlobalSearchKey) } }
    val onOpenDanmaku: () -> Unit = remember(navigator) { { navigator.push(DanmakuSettingsKey) } }
    val onOpenStats: () -> Unit = remember(navigator) { { navigator.push(ReadingStatsKey) } }
    val onOpenTrack: () -> Unit = remember(navigator) { { navigator.push(TrackSettingsKey) } }
    val onOpenSettings: () -> Unit = remember(navigator) { { navigator.push(SettingsKey) } }
    val onOpenSource: (SourceRuntimeInfo) -> Unit = remember(navigator) {
        { info -> navigator.push(SourceSearchKey(info.id, info.name, info.mediaType)) }
    }
    val onOpenScreen: (MiyomuKey) -> Unit = remember(navigator) { { key -> navigator.push(key) } }

    val badgeCount: (HomeTab) -> Int = { tab ->
        when (tab) {
            HomeTab.Novels -> novelBooks.size
            HomeTab.Library -> books.size
            else -> 0
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp

        val content: @Composable () -> Unit = {
            HomeTabContent(
                tab = selected,
                graph = graph,
                onOpenBook = onOpenBook,
                onOpenHistoryResume = onHistoryResume,
                onOpenHistoryDetails = onHistoryOpenDetails,
                onOpenSources = onOpenSources,
                onOpenDownloads = onOpenDownloads,
                onOpenBackup = onOpenBackup,
                onOpenGlobalSearch = onOpenGlobalSearch,
                onOpenDanmaku = onOpenDanmaku,
                onOpenStats = onOpenStats,
                onOpenTrack = onOpenTrack,
                onOpenSettings = onOpenSettings,
                onOpenSource = onOpenSource,
                onOpenScreen = onOpenScreen,
            )
        }

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
                    tabs.forEach { tab ->
                        NavigationRailItem(
                            selected = tab == selected,
                            onClick = { selectedIndex = tab.ordinal },
                            icon = { TabIcon(tab, badgeCount(tab)) },
                            label = { Text(tab.title()) },
                            alwaysShowLabel = true,
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize().weight(1f)) { content() }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = tab == selected,
                                onClick = { selectedIndex = tab.ordinal },
                                icon = { TabIcon(tab, badgeCount(tab)) },
                                label = { Text(tab.title()) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                },
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) { content() }
            }
        }
    }
}

/** The six shelves; this order is the order in the rail/bar. */
private enum class HomeTab {
    Novels,
    Library,
    Updates,
    History,
    Browse,
    More,
}

@Composable
private fun HomeTabContent(
    tab: HomeTab,
    graph: MiyomuGraph,
    onOpenBook: (LocalBook) -> Unit,
    onOpenHistoryResume: (String, String) -> Unit,
    onOpenHistoryDetails: (String) -> Unit,
    onOpenSources: () -> Unit,
    onOpenDownloads: (() -> Unit)?,
    onOpenBackup: (() -> Unit)?,
    onOpenGlobalSearch: () -> Unit,
    onOpenDanmaku: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenTrack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSource: (SourceRuntimeInfo) -> Unit,
    onOpenScreen: (MiyomuKey) -> Unit,
) {
    when (tab) {
        HomeTab.Novels -> NovelLibraryTab(graph, onOpenBook)
        HomeTab.Library -> MangaLibraryTab(graph, onOpenBook)
        HomeTab.Updates -> UpdatesTab(graph, onOpenHistoryDetails)
        HomeTab.History -> MangaHistoryTab(graph, onOpenHistoryResume, onOpenHistoryDetails)
        HomeTab.Browse -> BrowseTab(
            graph = graph,
            onOpenSource = onOpenSource,
            onManageSources = onOpenSources,
            onOpenGlobalSearch = onOpenGlobalSearch,
            onOpenScreen = onOpenScreen,
        )
        HomeTab.More -> MoreTab(
            graph = graph,
            onOpenSources = onOpenSources,
            onOpenDownloads = onOpenDownloads,
            onOpenBackup = onOpenBackup,
            onOpenDanmaku = onOpenDanmaku,
            onOpenStats = onOpenStats,
            onOpenTrack = onOpenTrack,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun HomeTab.title(): String = when (this) {
    HomeTab.Novels -> stringResource(MR.strings.label_novels)
    HomeTab.Library -> stringResource(MR.strings.label_library)
    HomeTab.Updates -> "更新"
    HomeTab.History -> "历史"
    HomeTab.Browse -> "浏览"
    HomeTab.More -> "更多"
}

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.Novels -> Icons.Outlined.ImportContacts
    HomeTab.Library -> Icons.AutoMirrored.Outlined.MenuBook
    HomeTab.Updates -> Icons.Outlined.Update
    HomeTab.History -> Icons.Outlined.History
    HomeTab.Browse -> Icons.Outlined.Explore
    HomeTab.More -> Icons.Outlined.MoreHoriz
}

@Composable
private fun TabIcon(tab: HomeTab, badgeCount: Int) {
    val icon = tab.icon()
    if (badgeCount > 0) {
        BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
            Icon(icon, contentDescription = tab.title())
        }
    } else {
        Icon(icon, contentDescription = tab.title())
    }
}
