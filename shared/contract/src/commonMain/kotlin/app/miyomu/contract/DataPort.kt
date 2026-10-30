package app.miyomu.contract

/**
 * Everything the UI needs from persistence, as one interface.
 *
 * `MiyomuData` implements this; the UI only ever sees the port, which is why `:shared:ui-core`
 * can depend on `:shared:data` with `implementation` instead of leaking the Room-backed aggregate
 * into every feature module.
 */
interface DataPort {
    val library: LibraryRepository
    val progress: ProgressRepository
    val anime: AnimeRepository
    val chapters: ChapterRepository
    val videoResolve: VideoResolveRepository
    val backup: BackupRepository
    val categories: CategoryRepository
    val libraryUpdates: LibraryUpdateRepository

    fun close()
}
