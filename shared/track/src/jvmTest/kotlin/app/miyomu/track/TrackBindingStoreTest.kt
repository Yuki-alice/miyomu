package app.miyomu.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrackBindingStoreTest {

    private fun store() = TrackBindingStore(read = { backing }, write = { backing = it })

    private var backing: String = ""

    @Test
    fun `upserts and reads back a binding`() {
        val store = store()
        assertNull(store.bindingFor("local:book-1"))

        store.upsert(
            TrackBinding("local:book-1", TrackTarget.ANILIST, remoteId = 42, remoteTitle = "作品", progress = 3),
        )

        val binding = store.bindingFor("local:book-1")!!
        assertEquals(TrackTarget.ANILIST, binding.target)
        assertEquals(42, binding.remoteId)
        assertEquals(3, binding.progress)
    }

    @Test
    fun `one active binding per item - rebinding replaces`() {
        val store = store()
        store.upsert(TrackBinding("item", TrackTarget.ANILIST, 1, "AniList 条目"))
        store.upsert(TrackBinding("item", TrackTarget.BANGUMI, 99, "Bangumi 条目"))

        val binding = store.bindingFor("item")!!
        assertEquals(TrackTarget.BANGUMI, binding.target)
        assertEquals(1, store.all().size)
    }

    @Test
    fun `progress pushes update in place`() {
        val store = store()
        store.upsert(TrackBinding("item", TrackTarget.ANILIST, 42, "作品", progress = 1))
        store.upsert(TrackBinding("item", TrackTarget.ANILIST, 42, "作品", progress = 5))

        assertEquals(5, store.bindingFor("item")!!.progress)
        assertEquals(1, store.all().size)
    }

    @Test
    fun `remove drops only the target item`() {
        val store = store()
        store.upsert(TrackBinding("a", TrackTarget.ANILIST, 1, "A"))
        store.upsert(TrackBinding("b", TrackTarget.BANGUMI, 2, "B"))

        store.remove("a")

        assertNull(store.bindingFor("a"))
        assertEquals("B", store.bindingFor("b")!!.remoteTitle)
    }

    @Test
    fun `corrupt storage degrades to empty`() {
        val store = TrackBindingStore(read = { "not-json" }, write = {})
        assertEquals(emptyList(), store.all())
        assertNull(store.bindingFor("item"))
    }
}
