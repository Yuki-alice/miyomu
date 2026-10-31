package app.miyomu.core.ui.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun MiyomuBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses the in-screen back button; edge-swipe integration comes later.
}
