package app.miyomu.domain.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.getEnum
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonPreferenceStoreTest {

    private fun newStore(dir: File, name: String = "prefs.json"): JsonPreferenceStore =
        JsonPreferenceStore(File(dir, name), CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `round trips values of every supported type`(@TempDir tempDir: File) = runTest {
        val store = newStore(tempDir)

        store.getString("k.string", "a").set("hello")
        store.getLong("k.long", 0).set(42L)
        store.getInt("k.int", 0).set(7)
        store.getFloat("k.float", 0f).set(0.5f)
        store.getBoolean("k.bool", false).set(true)
        store.getStringSet("k.set").set(setOf("x", "y"))
        store.getEnum("k.enum", Thread.State.NEW).set(Thread.State.RUNNABLE)

        assertEquals("hello", store.getString("k.string", "a").get())
        assertEquals(42L, store.getLong("k.long", 0).get())
        assertEquals(7, store.getInt("k.int", 0).get())
        assertEquals(0.5f, store.getFloat("k.float", 0f).get())
        assertEquals(true, store.getBoolean("k.bool", false).get())
        assertEquals(setOf("x", "y"), store.getStringSet("k.set").get())
        assertEquals(Thread.State.RUNNABLE, store.getEnum("k.enum", Thread.State.NEW).get())
    }

    @Test
    fun `persists across reopen and falls back on corruption`(@TempDir tempDir: File) = runTest {
        val file = File(tempDir, "prefs.json")
        val store = JsonPreferenceStore(file, CoroutineScope(Dispatchers.Unconfined))
        store.getInt("counter", 0).set(5)
        store.getBoolean("flag", false).set(true)

        // The async write runs eagerly on Unconfined; wait until it lands on disk.
        repeat(25) { if (file.isFile) return@repeat; Thread.sleep(20) }
        assertTrue(file.isFile)

        val reopened = JsonPreferenceStore(file, CoroutineScope(Dispatchers.Unconfined))
        assertEquals(5, reopened.getInt("counter", 0).get())
        assertEquals(true, reopened.getBoolean("flag", false).get())

        file.writeText("not json")
        val corrupted = JsonPreferenceStore(file, CoroutineScope(Dispatchers.Unconfined))
        assertEquals(0, corrupted.getInt("counter", 0).get())
    }

    @Test
    fun `delete restores default and changes flow emits updates`(@TempDir tempDir: File) = runTest {
        val store = newStore(tempDir)
        val pref = store.getInt("page", 1)

        // StateFlow-based changes conflate rapid updates, so assert a single observable update.
        val latest = async { pref.changes().take(2).last() }
        runCurrent()
        pref.set(2)
        assertEquals(2, latest.await())

        pref.delete()
        assertEquals(1, pref.get())
        assertFalse(pref.isSet())
    }
}
