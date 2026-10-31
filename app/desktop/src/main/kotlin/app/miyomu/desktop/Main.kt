package app.miyomu.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.miyomu.MiyomuApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Miyomu",
        state = rememberWindowState(width = 1280.dp, height = 820.dp),
    ) {
        MiyomuApp()
    }
}
