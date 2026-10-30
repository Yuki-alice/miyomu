package app.miyomu.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.miyomu.application.MiyomuAppEnvironment
import app.miyomu.application.MiyomuApplication
import app.miyomu.data.AndroidMiyomuDatabaseFactory
import app.miyomu.local.FileLocalFileRef
import app.miyomu.local.SafLocalFileRef
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current.applicationContext
            val platformPrefs = remember {
                context.getSharedPreferences("miyomu-platform", Context.MODE_PRIVATE)
            }
            val dataFactory = remember { AndroidMiyomuDatabaseFactory(context) }
            val libraryDir = remember { File(context.filesDir, "library").apply { mkdirs() } }
            var safTreeUri by remember {
                mutableStateOf(platformPrefs.getString("local_library_tree_uri", null)?.let(Uri::parse))
            }
            val root = remember(safTreeUri, libraryDir) {
                safTreeUri
                    ?.takeIf { SafLocalFileRef.isAccessible(context, it) }
                    ?.let { SafLocalFileRef.forTree(context, it) }
                    ?: FileLocalFileRef(libraryDir)
            }
            val directoryPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree(),
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                    platformPrefs.edit().putString("local_library_tree_uri", uri.toString()).apply()
                    safTreeUri = uri
                }
            }
            val environment = remember(root, dataFactory) {
                MiyomuAppEnvironment(
                    dataDir = context.filesDir,
                    libraryRoot = root,
                    databaseFactory = dataFactory,
                    deviceId = "android",
                    nowMillis = System::currentTimeMillis,
                    onPickLibraryDirectory = { directoryPicker.launch(null) },
                    onExit = { finish() },
                )
            }
            MiyomuApplication(environment)
        }
    }
}
