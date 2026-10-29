package eu.kanade.tachiyomi.ui.browse.manga.extension.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen

/**
 * CMP port stub: the real per-source preferences screen arrives with the settings port;
 * it is only reachable for ConfigurableSource, which the local source is not.
 */
class MangaSourcePreferencesScreen(
    private val sourceId: Long,
) : Screen {

    @Composable
    override fun Content() {
        Text(
            text = "Source preferences are not available yet (source #$sourceId).",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
