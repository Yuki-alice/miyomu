package app.miyomu.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProgressKind {
    PAGE,
    SCROLL,
    TIMESTAMP,
}

@Serializable
data class ReadingProgress(
    val itemId: String,
    val entryId: String,
    val kind: ProgressKind,
    val position: Double,
    val duration: Double? = null,
    val updatedAt: Long,
    val deviceId: String,
)
