package tachiyomi.presentation.core.util

import androidx.compose.runtime.Composable

/** Whether the software keyboard is currently visible; always false where no IME exists. */
@Composable
expect fun rememberImeVisibility(): Boolean
