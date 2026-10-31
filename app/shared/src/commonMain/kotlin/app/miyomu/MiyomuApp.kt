package app.miyomu

import androidx.compose.runtime.Composable
import app.miyomu.core.design.MiyomuTheme
import app.miyomu.core.ui.AppDestination
import app.miyomu.core.ui.MiyomuAppShell
import app.miyomu.core.ui.navigation.MiyomuBackHandler
import app.miyomu.core.ui.navigation.MiyomuNavHost
import app.miyomu.core.ui.navigation.MiyomuRoute
import app.miyomu.core.ui.navigation.rememberMiyomuNavigator
import app.miyomu.feature.discover.DiscoverScreen
import app.miyomu.feature.library.DetailsScreen
import app.miyomu.feature.library.HomeScreen
import app.miyomu.feature.library.LibraryScreen
import app.miyomu.feature.settings.SettingsScreen

@Composable
fun MiyomuApp() {
    MiyomuTheme {
        val navigator = rememberMiyomuNavigator()
        MiyomuBackHandler(enabled = navigator.canGoBack) { navigator.pop() }
        MiyomuAppShell(
            selected = navigator.current.toDestination(),
            onSelect = { navigator.selectTopLevel(it.toRoute()) },
        ) {
            MiyomuNavHost(navigator) { route ->
                when (route) {
                    MiyomuRoute.Home -> HomeScreen(
                        onOpenDetails = { navigator.push(MiyomuRoute.Details(it)) },
                    )
                    MiyomuRoute.Library -> LibraryScreen(
                        onOpenDetails = { navigator.push(MiyomuRoute.Details(it)) },
                    )
                    MiyomuRoute.Browse -> DiscoverScreen()
                    MiyomuRoute.Updates -> LibraryScreen(
                        onOpenDetails = { navigator.push(MiyomuRoute.Details(it)) },
                    )
                    MiyomuRoute.More -> SettingsScreen()
                    is MiyomuRoute.Details -> DetailsScreen(
                        contentId = route.contentId,
                        onBack = { navigator.pop() },
                    )
                }
            }
        }
    }
}

private fun MiyomuRoute.toDestination(): AppDestination = when (this) {
    MiyomuRoute.Home -> AppDestination.HOME
    MiyomuRoute.Library, is MiyomuRoute.Details -> AppDestination.LIBRARY
    MiyomuRoute.Browse -> AppDestination.BROWSE
    MiyomuRoute.Updates -> AppDestination.UPDATES
    MiyomuRoute.More -> AppDestination.MORE
}

private fun AppDestination.toRoute(): MiyomuRoute = when (this) {
    AppDestination.HOME -> MiyomuRoute.Home
    AppDestination.LIBRARY -> MiyomuRoute.Library
    AppDestination.BROWSE -> MiyomuRoute.Browse
    AppDestination.UPDATES -> MiyomuRoute.Updates
    AppDestination.MORE -> MiyomuRoute.More
}
