package app.miyomu.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/**
 * Small, dependency-free back stack used until Navigation Compose can be resolved
 * in this environment. It has the same semantics the app needs: one top-level
 * destination plus pushed details.
 */
class MiyomuNavigator(start: MiyomuRoute = MiyomuRoute.Home) {
    private val stack = mutableStateListOf(start)

    val current: MiyomuRoute
        get() = stack.last()

    val canGoBack: Boolean
        get() = stack.size > 1

    fun push(route: MiyomuRoute) {
        if (route == current) return
        stack.add(route)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun selectTopLevel(route: MiyomuRoute) {
        stack.clear()
        stack.add(route)
    }
}

@Composable
fun rememberMiyomuNavigator(start: MiyomuRoute = MiyomuRoute.Home): MiyomuNavigator =
    remember { MiyomuNavigator(start) }
