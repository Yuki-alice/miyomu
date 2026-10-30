package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Local-copy implementation of PreferenceStore for tests and previews.
 *
 * Values live in a mutable backing map and every returned preference writes through to it, so a
 * `set()` survives the call. The previous version handed out detached preference instances whose
 * writes were dropped, which silently broke anything that imports data and reads it back — a
 * source import followed by a lookup would come back empty.
 */
class InMemoryPreferenceStore(
    initialPreferences: Sequence<InMemoryPreference<*>> = sequenceOf(),
) : PreferenceStore {

    private val values: MutableMap<String, Any?> =
        initialPreferences.associate { it.key() to it.get() }.toMutableMap()

    override fun getString(key: String, defaultValue: String): Preference<String> =
        MemoryPreference(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
        MemoryPreference(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> =
        MemoryPreference(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
        MemoryPreference(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        MemoryPreference(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        MemoryPreference(key, defaultValue)

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = MemoryPreference(key, defaultValue)

    override fun getAll(): Map<String, *> = values.toMap()

    private inner class MemoryPreference<T>(
        private val name: String,
        private val fallback: T,
    ) : Preference<T> {

        @Suppress("UNCHECKED_CAST")
        override fun get(): T = values[name] as? T ?: fallback

        override fun set(value: T) {
            values[name] = value
        }

        override fun key(): String = name

        override fun isSet(): Boolean = values.containsKey(name)

        override fun delete() {
            values.remove(name)
        }

        override fun defaultValue(): T = fallback

        override fun changes(): Flow<T> = flow { emit(get()) }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> =
            changes().stateIn(scope, SharingStarted.Eagerly, get())
    }

    /** Seed entry used by tests and previews to pre-populate a value. */
    class InMemoryPreference<T>(
        private val key: String,
        private var data: T?,
        private val defaultValue: T,
    ) : Preference<T> {
        override fun key(): String = key

        override fun get(): T = data ?: defaultValue()

        override fun isSet(): Boolean = data != null

        override fun delete() {
            data = null
        }

        override fun defaultValue(): T = defaultValue

        override fun changes(): Flow<T> = flow { emit(get()) }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> {
            return changes().stateIn(scope, SharingStarted.Eagerly, get())
        }

        override fun set(value: T) {
            data = value
        }
    }
}
