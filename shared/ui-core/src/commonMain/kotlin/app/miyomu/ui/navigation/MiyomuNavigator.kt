package app.miyomu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.json.Json

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
 * Round-trips the back stack through its [MiyomuKey] serializers so the stack survives both
 * configuration changes (rotation) and process death. A stack that fails to decode (e.g. a key
 * shape that changed between versions) falls back to the root instead of crashing on restore.
 */
internal fun encodeMiyomuBackStack(stack: List<MiyomuKey>): String = Json.encodeToString(stack.toList())

internal fun decodeMiyomuBackStack(encoded: String): SnapshotStateList<MiyomuKey> {
    val keys = runCatching { Json.decodeFromString<List<MiyomuKey>>(encoded) }
        .getOrDefault(listOf(HomeKey))
    return SnapshotStateList<MiyomuKey>().apply { addAll(keys) }
}

internal val MiyomuBackStackSaver: Saver<SnapshotStateList<MiyomuKey>, String> = Saver(
    save = { stack -> encodeMiyomuBackStack(stack) },
    restore = { encoded -> decodeMiyomuBackStack(encoded) },
)

/**
 * Creates the back stack plus its navigator. The stack is `rememberSaveable`: every [MiyomuKey]
 * is `@Serializable`, so rotation and process death restore the exact destination the user was
 * on, including the reader position carried in the key.
 */
@Composable
fun rememberMiyomuNavigator(start: MiyomuKey = HomeKey): Pair<MiyomuNavigator, List<MiyomuKey>> {
    val backStackState = rememberSaveable(stateSaver = MiyomuBackStackSaver) {
        mutableStateOf(mutableStateListOf(start))
    }
    val backStack = backStackState.value
    val navigator = remember(backStack) { MiyomuNavigator(backStack) }
    return navigator to backStack
}

val LocalMiyomuNavigator = compositionLocalOf<MiyomuNavigator> {
    error("MiyomuNavigator was not provided; wrap the tree in MiyomuNavHost")
}
