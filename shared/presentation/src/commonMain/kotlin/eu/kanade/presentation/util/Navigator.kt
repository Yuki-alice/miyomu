package eu.kanade.presentation.util

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.ScreenModelStore
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus

/**
 * For invoking back press to the parent activity
 */
val LocalBackPress: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

/**
 * A variant of ScreenModel.coroutineScope except with the IO dispatcher instead of the
 * main dispatcher.
 */
val ScreenModel.ioCoroutineScope: CoroutineScope
    get() = ScreenModelStore.getOrPutDependency(
        screenModel = this,
        name = "ScreenModelIoCoroutineScope",
        factory = { key -> CoroutineScope(Dispatchers.IO + SupervisorJob()) + CoroutineName(key) },
        onDispose = { scope -> scope.cancel() },
    )

interface AssistContentScreen {
    fun onProvideAssistUrl(): String?
}
