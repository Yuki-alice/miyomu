package app.miyomu.ui.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AR-01 regression net: the saved back stack must round-trip every key type and degrade to the
 * root instead of crashing when a stored stack cannot be decoded.
 */
class MiyomuBackStackSaverTest {

    @Test
    fun `stack round-trips through the encoder`() {
        val original = SnapshotStateList<MiyomuKey>().apply {
            add(HomeKey)
            add(MangaDetailKey("book-1"))
            add(MangaReaderKey("book-1", chapterIndex = 3, pageIndex = 12))
            add(NovelReaderKey("novel-1", chapterIndex = 1, charOffset = 240))
            add(SourceDetailKey("sourceId", "Source", "comic-1", title = "Title", coverUrl = null))
            add(SettingsKey)
        }

        val restored = decodeMiyomuBackStack(encodeMiyomuBackStack(original))

        // SnapshotStateList compares by identity; compare the element lists instead.
        assertEquals(original.toList(), restored.toList())
    }

    @Test
    fun `undecodable stack falls back to the root`() {
        val restored = decodeMiyomuBackStack("not-a-back-stack")

        assertEquals(listOf<MiyomuKey>(HomeKey), restored)
    }
}
