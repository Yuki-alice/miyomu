package app.miyomu.core.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MiyomuNavHost(
    navigator: MiyomuNavigator,
    modifier: Modifier = Modifier,
    content: @Composable (MiyomuRoute) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        content(navigator.current)
    }
}
