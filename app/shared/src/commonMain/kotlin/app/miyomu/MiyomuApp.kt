package app.miyomu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.miyomu.core.design.MiyomuTheme
import app.miyomu.core.ui.AppDestination
import app.miyomu.core.ui.MiyomuAppShell
import app.miyomu.feature.discover.DiscoverScreen
import app.miyomu.feature.library.HomeScreen
import app.miyomu.feature.library.LibraryScreen
import app.miyomu.feature.settings.SettingsScreen

@Composable
fun MiyomuApp() {
    MiyomuTheme {
        var selected by remember { mutableStateOf(AppDestination.HOME) }
        MiyomuAppShell(selected = selected, onSelect = { selected = it }) { destination ->
            when (destination) {
                AppDestination.HOME -> HomeScreen()
                AppDestination.LIBRARY -> LibraryScreen()
                AppDestination.BROWSE -> DiscoverScreen()
                AppDestination.UPDATES -> LibraryScreen()
                AppDestination.MORE -> SettingsScreen()
            }
        }
    }
}
