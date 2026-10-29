package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import tachiyomi.domain.items.chapter.model.NoChaptersException
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.net.UnknownHostException

/**
 * CMP port of Aniyomi's `context(context: Context) val Throwable.formattedMessage`.
 *
 * Three things do not survive the port and are dropped rather than faked:
 * - the `Context` receiver (moko resolves strings from the composition, not a Context),
 * - the `isOnline()` check (Android connectivity manager; there is no KMP equivalent yet, so a
 *   failing host always reports `exception_unknown_host`),
 * - the `HttpException`, torrent (`DisabledTorrServerException`), anime (`NoEpisodesException`)
 *   and `SourceNotInstalledException` branches, because those types live in modules that are not
 *   ported. Re-add them with their modules in Phase B.
 */
@Composable
fun Throwable.formattedMessage(): String {
    when (this) {
        is UnknownHostException -> return stringResource(MR.strings.exception_unknown_host, message ?: "")
        is NoChaptersException -> return stringResource(MR.strings.no_results_found)
    }
    val className = this::class.simpleName
    return when (className) {
        "Exception", "IOException" -> message ?: className
        else -> "$className: $message"
    }
}
