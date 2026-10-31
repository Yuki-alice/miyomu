package app.miyomu.core.ui.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun MiyomuBackHandler(enabled: Boolean, onBack: () -> Unit)
