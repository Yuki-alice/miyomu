package app.miyomu.data

import app.miyomu.contract.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * File-level backup: serializes the data layer snapshot to JSON and restores it. Preferences
 * ride along as a raw store snapshot ([preferencesJson]) since P1-5 偏好同步; library,
 * progress and read/seen/bookmark marks are covered.
 */
class BackupManager(
    private val data: DataPort,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(nowMillis: Long, preferencesJson: String? = null): String {
        val snapshot = data.backup.export()
            .copy(exportedAt = nowMillis, preferences = preferencesJson)
        return json.encodeToString(snapshot)
    }

    suspend fun restore(jsonText: String, replace: Boolean): RestoreResult {
        val snapshot = runCatching { json.decodeFromString<BackupData>(jsonText) }
            .getOrElse { error("备份文件无法解析：${it.message}") }
        require(snapshot.version == SUPPORTED_VERSION) { "不支持的备份版本：${snapshot.version}" }
        data.backup.restore(snapshot, replace)
        return RestoreResult(
            library = snapshot.library.size,
            progress = snapshot.progress.size,
            chapters = snapshot.chapters.size,
            episodes = snapshot.episodes.size,
            preferences = snapshot.preferences,
        )
    }

    fun writeToFile(jsonText: String, target: File) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(jsonText)
        if (!tmp.renameTo(target)) {
            target.writeText(jsonText)
            tmp.delete()
        }
    }

    fun readFromFile(file: File): String = file.readText()

    data class RestoreResult(
        val library: Int,
        val progress: Int,
        val chapters: Int,
        val episodes: Int,
        /** Raw preference snapshot from the backup, applied by the caller (P1-5). */
        val preferences: String? = null,
    )

    companion object {
        const val SUPPORTED_VERSION = 1
    }
}
