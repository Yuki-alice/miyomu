package app.miyomu.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceDescriptor(
    val id: String,
    val name: String,
    val mediaType: MediaType,
    val version: String,
    val apiVersion: Int = 1,
    val minAppVersion: String? = null,
    val url: String? = null,
)
