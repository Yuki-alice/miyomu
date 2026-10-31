package app.miyomu

import androidx.compose.ui.window.ComposeUIViewController
import app.miyomu.core.database.createMiyomuDatabase
import platform.Foundation.NSHomeDirectory
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val path = NSHomeDirectory() + "/Documents/miyomu.db"
    val database = createMiyomuDatabase(path)
    return ComposeUIViewController { MiyomuApp(database) }
}
