package app.miyomu.track

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persistence for manual per-item tracker bindings (P1-9). Storage is a JSON preference,
 * accessed through injected read/write lambdas so this class stays decoupled from any
 * concrete preference API; the settings/preferences layer supplies the two accessors.
 */
class TrackBindingStore(
    private val read: () -> String,
    private val write: (String) -> Unit,
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun all(): List<TrackBinding> =
        runCatching {
            json.decodeFromString(ListSerializer(TrackBinding.serializer()), read())
        }.getOrDefault(emptyList())

    /** Latest binding for [itemId] regardless of target (one active binding per item). */
    fun bindingFor(itemId: String): TrackBinding? =
        all().filter { it.itemId == itemId }.maxByOrNull { it.progress }

    fun upsert(binding: TrackBinding) {
        val rest = all().filterNot { it.itemId == binding.itemId }
        write(json.encodeToString(ListSerializer(TrackBinding.serializer()), rest + binding))
    }

    fun remove(itemId: String) {
        write(json.encodeToString(ListSerializer(TrackBinding.serializer()), all().filterNot { it.itemId == itemId }))
    }
}
