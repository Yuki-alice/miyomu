package app.miyomu.model

import kotlinx.serialization.Serializable

@Serializable
data class Paged<T>(
    val items: List<T>,
    val hasNext: Boolean,
)
