package app.miyomu.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.miyomu.data.AndroidMiyomuDatabaseFactory
import app.miyomu.data.createMiyomuData
import app.miyomu.domain.preferences.JsonPreferenceStore
import app.miyomu.local.FileLocalComicRepository
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.novel.LocalNovelRepository
import app.miyomu.novel.NovelLibraryRepository
import app.miyomu.network.cookies.FileCookiesStorage
import app.miyomu.network.createMiyomuHttpClient
import app.miyomu.ui.MiyomuApp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
            val novels = remember { LocalNovelRepository() }
            val novelLibrary = remember { NovelLibraryRepository() }
            val preferences = remember { JsonPreferenceStore(File(context.filesDir, "preferences.json")) }
            val novelClient = remember {
                createMiyomuHttpClient(FileCookiesStorage(kotlinx.io.files.Path(File(context.filesDir, "cookies.json").absolutePath)))
            }
            val novelFetcher: suspend (String) -> String = { url ->
                novelClient.get(url).bodyAsText()
            }

            DisposableEffect(data) {
                onDispose { data.close() }
            }

            MiyomuApp(
                library = library,
                comics = comics,
                novels = novels,
                novelLibrary = novelLibrary,
                data = data,
                root = root,
                deviceId = "android",
                nowMillis = System::currentTimeMillis,
                preferenceStore = preferences,
                novelFetcher = novelFetcher,
            )
        }
    }
}
