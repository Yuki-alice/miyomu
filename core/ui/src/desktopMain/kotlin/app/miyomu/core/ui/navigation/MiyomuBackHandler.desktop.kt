package app.miyomu.core.ui.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun MiyomuBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop back is handled by the in-screen back button for now.
}
