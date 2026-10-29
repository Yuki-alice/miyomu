package app.miyomu.ui

import app.miyomu.data.MiyomuData
import app.miyomu.local.LocalComicRepository
import app.miyomu.local.LocalFileRef
import app.miyomu.local.LocalLibraryRepository

/**
 * Constructor-injection replacement for Aniyomi's Injekt graph: one instance wired by the
 * platform entry points, consumed by tabs and screens.
 */
class MiyomuGraph(
    val library: LocalLibraryRepository,
    val comics: LocalComicRepository,
    val data: MiyomuData,
    val root: LocalFileRef,
    val deviceId: String,
    val nowMillis: () -> Long,
    val onOpenLibraryFolder: (() -> Unit)? = null,
)
