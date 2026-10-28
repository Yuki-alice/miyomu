package org.projectv.spike

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.projectv.spike.model.DemoComic
import org.projectv.spike.model.demoComics
import org.projectv.spike.screens.DetailScreen
import org.projectv.spike.screens.LibraryScreen
import org.projectv.spike.screens.SettingsScreen

private enum class Section(val label: String, val icon: ImageVector) {
    Library("Library", Icons.Default.Home),
    Detail("Detail", Icons.Default.Info),
    Settings("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    ProjectVTheme {
        var section by remember { mutableStateOf(Section.Library) }
        var selected by remember { mutableStateOf<DemoComic>(demoComics.first()) }

        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                header = {
                    Text(
                        text = "PV",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                },
            ) {
                Spacer(Modifier.weight(1f))
                Section.entries.forEach { item ->
                    NavigationRailItem(
                        selected = section == item,
                        onClick = { section = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        modifier = Modifier.testTag("nav-${item.label.lowercase()}"),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            VerticalDivider()
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { TopAppBar(title = { Text(section.label) }) },
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().padding(innerPadding)) {
                    when (section) {
                        Section.Library -> LibraryScreen(
                            comics = demoComics,
                            onOpen = {
                                selected = it
                                section = Section.Detail
                            },
                        )

                        Section.Detail -> DetailScreen(comic = selected)
                        Section.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}
