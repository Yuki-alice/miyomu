package app.miyomu.source.runtime

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal interface SourceKeyValueStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun delete(key: String)
    suspend fun flush()
}

internal class FileSourceKeyValueStore(
    private val file: Path,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : SourceKeyValueStore {

    private val values = mutableMapOf<String, String>()
    private var opened = false

    suspend fun open() {
        if (opened) return
        opened = true

        val text = readTextIfExists(file) ?: return
        if (text.isBlank()) return

        val restored = runCatching { json.decodeFromString<Map<String, String>>(text) }.getOrNull()
        if (restored == null) {
            moveToCorrupt(file)
            return
        }
        values.putAll(restored)
    }

    override fun get(key: String): String? = values[key]

    override fun set(key: String, value: String) {
        values[key] = value
    }

    override fun delete(key: String) {
        values.remove(key)
    }

    override suspend fun flush() {
        writeTextAtomically(file, json.encodeToString(values.toMap()))
    }

    private fun moveToCorrupt(file: Path) {
        val corrupt = Path(file.toString() + ".corrupt")
        runCatching { SystemFileSystem.atomicMove(file, corrupt) }
    }
}
