package app.miyomu.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository
import app.miyomu.ui.home.HomeScreen
import cafe.adriel.voyager.navigator.Navigator

@Composable
fun MiyomuApp(
    library: LocalLibraryRepository,
    comics: LocalComicRepository,
    data: MiyomuData,
    root: LocalFileRef,
    deviceId: String,
    nowMillis: () -> Long,
    onOpenLibraryFolder: (() -> Unit)? = null,
) {
    val graph = remember {
        MiyomuGraph(
            library = library,
            comics = comics,
            data = data,
            root = root,
            deviceId = deviceId,
            nowMillis = nowMillis,
            onOpenLibraryFolder = onOpenLibraryFolder,
        )
    }

    LaunchedEffect(root) {
        library.refresh(root)
    }

    MiyomuTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Navigator(HomeScreen(graph))
        }
    }
}
