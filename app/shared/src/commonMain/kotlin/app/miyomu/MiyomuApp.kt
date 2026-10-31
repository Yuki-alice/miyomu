package app.miyomu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.miyomu.core.design.MiyomuTheme
import app.miyomu.core.ui.AppDestination
import app.miyomu.core.ui.MiyomuAppShell
import app.miyomu.core.ui.navigation.MiyomuNavHost
import app.miyomu.core.ui.navigation.MiyomuRoute
import app.miyomu.core.ui.navigation.destinationRoute
import app.miyomu.feature.discover.DiscoverScreen
import app.miyomu.feature.library.DetailsScreen
import app.miyomu.feature.library.HomeScreen
import app.miyomu.feature.library.LibraryScreen
import app.miyomu.feature.settings.SettingsScreen

@Composable
fun MiyomuApp() {
    MiyomuTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        MiyomuAppShell(
            selected = currentRoute.toDestination(),
            onSelect = { navController.selectTopLevel(it) },
        ) {
            MiyomuNavHost(navController) { route ->
                when (route) {
                    MiyomuRoute.Home -> HomeScreen(
                        onOpenDetails = { navController.openDetails(it) },
                    )
                    MiyomuRoute.Library -> LibraryScreen(
                        onOpenDetails = { navController.openDetails(it) },
                    )
                    MiyomuRoute.Browse -> DiscoverScreen()
                    MiyomuRoute.Updates -> LibraryScreen(
                        onOpenDetails = { navController.openDetails(it) },
                    )
                    MiyomuRoute.More -> SettingsScreen()
                    is MiyomuRoute.Details -> DetailsScreen(
                        contentId = route.contentId,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun NavHostController.openDetails(contentId: String) {
    navigate(MiyomuRoute.Details(contentId).key)
}

private fun NavHostController.selectTopLevel(destination: AppDestination) {
    val route = destination.toRoute().key
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun String?.toDestination(): AppDestination = when {
    this == null -> AppDestination.HOME
    startsWith("details/") -> AppDestination.LIBRARY
    this == MiyomuRoute.Library.key -> AppDestination.LIBRARY
    this == MiyomuRoute.Browse.key -> AppDestination.BROWSE
    this == MiyomuRoute.Updates.key -> AppDestination.UPDATES
    this == MiyomuRoute.More.key -> AppDestination.MORE
    else -> AppDestination.HOME
}

private fun AppDestination.toRoute(): MiyomuRoute = when (this) {
    AppDestination.HOME -> MiyomuRoute.Home
    AppDestination.LIBRARY -> MiyomuRoute.Library
    AppDestination.BROWSE -> MiyomuRoute.Browse
    AppDestination.UPDATES -> MiyomuRoute.Updates
    AppDestination.MORE -> MiyomuRoute.More
}
