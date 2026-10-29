package app.miyomu.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ApplyReaderOrientation(flag: Int) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, flag) {
        activity?.requestedOrientation = flag
        onDispose {
            // Leaving the reader hands orientation control back to the system.
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
