package app.miyomu.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read

private const val DETAILS_PATTERN = "details/{contentId}"

@Composable
fun MiyomuNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    content: @Composable (MiyomuRoute) -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = MiyomuRoute.Home.key,
        modifier = modifier,
    ) {
        composable(MiyomuRoute.Home.key) { content(MiyomuRoute.Home) }
        composable(MiyomuRoute.Library.key) { content(MiyomuRoute.Library) }
        composable(MiyomuRoute.Browse.key) { content(MiyomuRoute.Browse) }
        composable(MiyomuRoute.Updates.key) { content(MiyomuRoute.Updates) }
        composable(MiyomuRoute.More.key) { content(MiyomuRoute.More) }
        composable(
            route = DETAILS_PATTERN,
            arguments = listOf(navArgument("contentId") { type = NavType.StringType }),
        ) { entry ->
            content(MiyomuRoute.Details(entry.arguments?.read { getString("contentId") }.orEmpty()))
        }
    }
}

val MiyomuRoute.destinationRoute: String
    get() = when (this) {
        is MiyomuRoute.Details -> DETAILS_PATTERN
        else -> key
    }
