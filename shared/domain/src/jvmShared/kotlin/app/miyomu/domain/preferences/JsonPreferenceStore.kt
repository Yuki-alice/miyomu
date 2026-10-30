package app.miyomu.domain.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

/**
 * File-backed [PreferenceStore] shaped after Aniyomi's AndroidPreferenceStore: every value is
 * stored as a string inside a single JSON object file, written atomically on change.
 */
class JsonPreferenceStore(
    private val file: File,
    private val persistenceScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : PreferenceStore {

    private val values = ConcurrentHashMap<String, String>()
    private val keyFlows = ConcurrentHashMap<String, MutableStateFlow<String?>>()
    private val writeMutex = Mutex()

    init {
        runCatching {
            if (file.isFile) {
                val element = Json.parseToJsonElement(file.readText())
                (element as? JsonObject)?.forEach { (key, value) ->
                    if (value is JsonPrimitive && value.isString) values[key] = value.content
                }
            }
        }
        // Corrupted preference files fall back to defaults rather than blocking startup.
    }

    override fun getString(key: String, defaultValue: String) =
        preference(key, defaultValue, { it }, { it })

    override fun getLong(key: String, defaultValue: Long) =
        preference(key, defaultValue, Long::toString, String::toLong)

    override fun getInt(key: String, defaultValue: Int) =
        preference(key, defaultValue, Int::toString, String::toInt)

    override fun getFloat(key: String, defaultValue: Float) =
        preference(key, defaultValue, Float::toString, String::toFloat)

    override fun getBoolean(key: String, defaultValue: Boolean) =
        preference(key, defaultValue, Boolean::toString, String::toBooleanStrict)

    override fun getStringSet(key: String, defaultValue: Set<String>) =
        preference(
            key,
            defaultValue,
            { it.joinToString(SET_SEPARATOR) },
            { it.split(SET_SEPARATOR).filter(String::isNotEmpty).toSet() },
        )

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ) = preference(key, defaultValue, serializer, deserializer)

    override fun getAll(): Map<String, *> = values.toMap()

    private fun <T> preference(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ) = JsonPreference(this, key, defaultValue, serializer, deserializer)

    internal fun observe(key: String): MutableStateFlow<String?> =
        keyFlows.computeIfAbsent(key) { MutableStateFlow(values[key]) }

    internal fun persist(key: String, value: String) {
        values[key] = value
        observe(key).value = value
        scheduleWrite()
    }

    /**
     * P1-5 偏好同步: serializes every preference (string-encoded values keyed by name) for
     * embedding into a backup; [importValuesJson] merges a snapshot back in.
     */
    fun exportValuesJson(): String = buildJsonObject {
        values.forEach { (key, value) -> put(key, kotlinx.serialization.json.JsonPrimitive(value)) }
    }.toString()

    fun importValuesJson(jsonText: String) {
        val element = runCatching { Json.parseToJsonElement(jsonText) }.getOrNull() as? JsonObject ?: return
        element.forEach { (key, value) ->
            if (value is JsonPrimitive && value.isString) persist(key, value.content)
        }
    }

    internal fun remove(key: String) {
        values.remove(key)
        observe(key).value = null
        scheduleWrite()
    }

    private fun scheduleWrite() {
        persistenceScope.launch { write() }
    }

    private suspend fun write() {
        writeMutex.withLock {
            runCatching {
                file.parentFile?.mkdirs()
                val tmp = File(file.parentFile, file.name + ".tmp")
                val obj = JsonObject(values.mapValues { (_, value) -> JsonPrimitive(value) })
                tmp.writeText(obj.toString())
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private companion object {
        const val SET_SEPARATOR = "\u0001"
    }
}

class JsonPreference<T>(
    private val store: JsonPreferenceStore,
    private val key: String,
    private val defaultValue: T,
    private val serializer: (T) -> String,
    private val deserializer: (String) -> T,
) : Preference<T> {

    private fun decode(raw: String?): T = raw
        ?.let { runCatching { deserializer(it) }.getOrNull() }
        ?: defaultValue

    override fun key(): String = key

    override fun get(): T = decode(store.observe(key).value)

    override fun set(value: T) {
        store.persist(key, serializer(value))
    }

    override fun isSet(): Boolean = store.getAll().containsKey(key)

    override fun delete() {
        store.remove(key)
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> = store.observe(key)
        .map { decode(it) }
        .distinctUntilChanged()

    override fun stateIn(scope: CoroutineScope) = changes()
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, get())

    override fun toString(): String = "JsonPreference(key=$key, value=${get()})"
}
