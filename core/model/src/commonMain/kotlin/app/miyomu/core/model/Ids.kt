package app.miyomu.core.model

import kotlin.jvm.JvmInline

/** Stable source identifier. Phase P0 keeps Kototoro's string names. */
@JvmInline
value class SourceId(val value: String)

/** Content id. Phase P0 keeps Kototoro's Long ids; composite ids come later. */
@JvmInline
value class ContentId(val value: Long)

/** Chapter id, scoped to a content by the repository layer. */
@JvmInline
value class ChapterId(val value: Long)
