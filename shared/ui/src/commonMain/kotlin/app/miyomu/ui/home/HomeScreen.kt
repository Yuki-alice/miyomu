package app.miyomu.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.miyomu.local.LocalBook
import app.miyomu.model.MediaType
import app.miyomu.ui.MiyomuGraph
import app.miyomu.ui.entries.MangaDetailScreen
import app.miyomu.ui.entries.VideoDetailScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator

/**
 * Port of Aniyomi's ui/home/HomeScreen.kt: a Voyager TabNavigator with the five bottom tabs,
 * adaptive NavigationBar (narrow) / NavigationRail (>=720dp), adapted to constructor injection.
 */
class HomeScreen(private val graph: MiyomuGraph) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val books by graph.library.books.collectAsState()

        val onOpenBook: (LocalBook) -> Unit = remember(navigator) {
            { book ->
                // Video books open the anime-style detail screen; comics use the manga one.
                if (book.mediaType == MediaType.VIDEO) {
                    navigator.push(VideoDetailScreen(graph, book))
                } else {
                    navigator.push(MangaDetailScreen(graph, book))
                }
            }
        }
        val tabs = remember(graph, onOpenBook) {
            listOf(
                MangaLibraryTab(graph, onOpenBook),
                UpdatesTab(graph),
                MangaHistoryTab(graph, onOpenBook),
                BrowseTab(graph),
                MoreTab(graph),
            )
        }

        TabNavigator(tabs.first()) { tabNavigator ->
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
                            tabs.forEach { tab ->
                                NavigationRailItem(
                                    selected = tabNavigator.current == tab,
                                    onClick = { tabNavigator.current = tab },
                                    icon = { TabIcon(tab, if (tab is MangaLibraryTab) books.size else 0) },
                                    label = { Text(tab.options.title) },
                                    alwaysShowLabel = true,
                                )
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            CurrentTab()
                        }
                    }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                tabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = tabNavigator.current == tab,
                                        onClick = { tabNavigator.current = tab },
                                        icon = { TabIcon(tab, if (tab is MangaLibraryTab) books.size else 0) },
                                        label = { Text(tab.options.title) },
                                        alwaysShowLabel = true,
                                    )
                                }
                            }
                        },
                    ) { padding ->
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            CurrentTab()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: Tab, badgeCount: Int) {
    val icon = tab.icon() ?: return
    if (badgeCount > 0) {
        BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
            Icon(icon, contentDescription = tab.options.title)
        }
    } else {
        Icon(icon, contentDescription = tab.options.title)
    }
}

private fun Tab.icon(): ImageVector? = when (this) {
    is MangaLibraryTab -> Icons.AutoMirrored.Outlined.MenuBook
    is UpdatesTab -> Icons.Outlined.Update
    is MangaHistoryTab -> Icons.Outlined.History
    is BrowseTab -> Icons.Outlined.Explore
    is MoreTab -> Icons.Outlined.MoreHoriz
    else -> null
}
