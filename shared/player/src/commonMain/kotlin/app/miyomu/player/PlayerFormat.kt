package app.miyomu.player

import org.openani.mediamp.metadata.AudioTrack
import org.openani.mediamp.metadata.SubtitleTrack

/** Playback speeds offered in the player settings sheet. */
internal val playerSpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

internal fun formatTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    fun Long.pad() = toString().padStart(2, '0')
    return if (hours > 0) {
        "${hours}:${minutes.pad()}:${seconds.pad()}"
    } else {
        "${minutes.pad()}:${seconds.pad()}"
    }
}

internal fun formatSpeed(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) "${speed.toInt()}.0" else speed.toString()
    return "${text}x"
}

/**
 * ExoPlayer and MPV both put the human-readable label in `labels` and repeat it in `name`;
 * fall back to the backend id only when a file carries no track metadata at all.
 */
internal fun AudioTrack.displayName(): String =
    labels.firstOrNull { it.value.isNotBlank() }?.value
        ?: name?.takeIf { it.isNotBlank() }
        ?: internalId

internal fun SubtitleTrack.displayName(): String =
    labels.firstOrNull { it.value.isNotBlank() }?.value
        ?: language?.takeIf { it.isNotBlank() }
        ?: internalId
