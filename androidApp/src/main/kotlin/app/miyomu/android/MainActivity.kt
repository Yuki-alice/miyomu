package app.miyomu.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.miyomu.data.AndroidMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.ui.MiyomuApp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current.applicationContext
            val data = remember {
                createMiyomuData(AndroidMiyomuDatabaseFactory(context))
            }
            val libraryRoot = remember {
                File(context.filesDir, "library").apply { mkdirs() }
            }
            val root = remember { FileLocalFileRef(libraryRoot) }
            val comics = remember { FileLocalComicRepository() }
            val library = remember { LocalLibraryRepository() }

            DisposableEffect(data) {
                onDispose { data.close() }
            }

            MiyomuApp(
                library = library,
                comics = comics,
                data = data,
                root = root,
                deviceId = "android",
                nowMillis = System::currentTimeMillis,
            )
        }
    }
}
