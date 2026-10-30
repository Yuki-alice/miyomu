package app.miyomu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * The single navigation surface for every screen.
 *
 * Navigation 3 deliberately ships no `NavController`: the back stack is a plain observable list
 * and navigation is a handful of list mutations. [MiyomuNavigator] is that mutation surface, kept
 * as an explicit constructor parameter (or read through [LocalMiyomuNavigator] inside a ported
 * screen) so a destination can never reach into a global controller.
 */
class MiyomuNavigator internal constructor(
    private val backStack: SnapshotStateList<MiyomuKey>,
) {
    val current: MiyomuKey
        get() = backStack.last()

    val size: Int
        get() = backStack.size

    val canPop: Boolean
        get() = backStack.size > 1

    /** Read-only view, useful for tests and for rendering "up" affordances. */
    val stack: List<MiyomuKey>
        get() = backStack.toList()

    fun push(key: MiyomuKey) {
        backStack.add(key)
    }

    /**
     * Pops one entry. Returns false at the root, where the platform is expected to finish the
     * activity/window instead of leaving an empty stack.
     */
    fun pop(): Boolean {
        if (!canPop) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** Pops back to [key] if it is on the stack, otherwise pushes it. */
    fun popUpTo(key: MiyomuKey) {
        val index = backStack.indexOfLast { it == key }
        if (index < 0) {
            push(key)
            return
        }
        while (backStack.lastIndex > index) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /** Swaps the top entry, used when a screen is replaced rather than stacked (e.g. next episode). */
    fun replace(key: MiyomuKey) {
        if (backStack.isEmpty()) {
            backStack.add(key)
        } else {
            backStack[backStack.lastIndex] = key
        }
    }

    /** Clears everything and starts again from [key]. */
    fun resetTo(key: MiyomuKey) {
        backStack.clear()
        backStack.add(key)
    }
}

/**
 * Creates the back stack plus its navigator. State lives in `remember` rather than
 * `rememberNavBackStack` because restoring requires every key to round-trip through a saved-state
 * serializer; that is a follow-up once the migration has settled.
 */
@Composable
fun rememberMiyomuNavigator(start: MiyomuKey = HomeKey): Pair<MiyomuNavigator, List<MiyomuKey>> {
    val backStack = remember { mutableStateListOf(start) }
    val navigator = remember(backStack) { MiyomuNavigator(backStack) }
    return navigator to backStack
}

val LocalMiyomuNavigator = compositionLocalOf<MiyomuNavigator> {
    error("MiyomuNavigator was not provided; wrap the tree in MiyomuNavHost")
}
