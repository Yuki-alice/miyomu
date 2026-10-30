package app.miyomu.data

import app.miyomu.contract.BackupData
import app.miyomu.network.WebDavNotFoundException
import app.miyomu.network.WebDavOps
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * P1-5 WebDAV 双向同步: compares a freshly exported local backup against the remote copy using
 * the last-synced marker, then uploads / downloads / reports a conflict accordingly.
 *
 * Decision matrix (local/remote `exportedAt` vs [Input.lastSyncedAt]):
 * - remote missing            → upload
 * - both changed              → [Outcome.Conflict] (UI picks a side, then calls [resolveConflict])
 * - remote changed only       → download + apply via [Input.applyRemote]
 * - local changed only        → upload
 * - neither changed           → up to date
 *
 * The caller persists the returned [Outcome.WonSide]'s `exportedAt` as the new last-synced
 * marker; the service itself owns no storage. Preferences ride inside the backup JSON
 * ([BackupData.preferences]), so 偏好同步 follows the same matrix — [Input.applyRemote]
 * applies both halves.
 */
class WebDavSyncService(
    private val ops: WebDavOps,
    private val fileUrl: String,
    private val username: String,
    private val password: String,
) {

    private val json = Json { ignoreUnknownKeys = true }

    data class Input(
        /** Fresh local export including the preferences snapshot. */
        val localJson: String,
        val lastSyncedAt: Long,
        /** Applies a downloaded backup JSON (restore + preference import). */
        val applyRemote: suspend (String) -> Unit,
    )

    sealed interface Outcome {
        /** Which side won; [exportedAt] is the new last-synced marker for the caller. */
        data class Won(val side: Side, val exportedAt: Long) : Outcome
        data object UpToDate : Outcome
        data class Conflict(val local: BackupData, val remote: BackupData) : Outcome

        enum class Side { LOCAL, REMOTE }
    }

    suspend fun sync(input: Input): Outcome {
        val remoteJson = fetchRemote()
        val localBackup = parse(input.localJson)
        val remoteBackup = remoteJson?.let(::parse)

        val remoteExportedAt = remoteBackup?.exportedAt ?: 0L
        val localExportedAt = localBackup.exportedAt
        val remoteChanged = remoteExportedAt > input.lastSyncedAt
        val localChanged = localExportedAt > input.lastSyncedAt

        return when {
            remoteBackup == null -> {
                ops.upload(fileUrl, username, password, input.localJson.encodeToByteArray())
                Outcome.Won(Outcome.Side.LOCAL, localExportedAt)
            }
            localChanged && remoteChanged -> Outcome.Conflict(localBackup, remoteBackup)
            remoteChanged -> {
                input.applyRemote(remoteJson)
                Outcome.Won(Outcome.Side.REMOTE, remoteExportedAt)
            }
            localChanged -> {
                ops.upload(fileUrl, username, password, input.localJson.encodeToByteArray())
                Outcome.Won(Outcome.Side.LOCAL, localExportedAt)
            }
            else -> Outcome.UpToDate
        }
    }

    /** Conflict resolution: keep local (upload) or take remote (download + apply). */
    suspend fun resolveConflict(
        keepLocal: Boolean,
        localJson: String,
        remoteJson: String,
        chosenExportedAt: Long,
    ): Outcome.Won {
        return if (keepLocal) {
            ops.upload(fileUrl, username, password, localJson.encodeToByteArray())
            Outcome.Won(Outcome.Side.LOCAL, chosenExportedAt)
        } else {
            Outcome.Won(Outcome.Side.REMOTE, chosenExportedAt)
        }
    }

    /** Fetches the raw remote backup (conflict resolution "使用云端" needs the JSON to apply). */
    suspend fun fetchRemoteJson(): String? =
        ops.download(fileUrl, username, password).decodeToString().takeIf { it.isNotBlank() }

    private suspend fun fetchRemote(): String? =
        runCatching { ops.download(fileUrl, username, password).decodeToString() }
            .recoverCatching { error ->
                // First run: the remote backup does not exist yet.
                if (error is app.miyomu.network.WebDavNotFoundException) "" else throw error
            }
            .getOrElse { error -> throw IllegalStateException("WebDAV 下载失败：${error.message}", error) }
            .takeIf { it.isNotBlank() }

    private fun parse(text: String): BackupData =
        runCatching { json.decodeFromString<BackupData>(text) }
            .getOrElse { throw IllegalStateException("备份解析失败：${it.message}", it) }
}
