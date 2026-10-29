package app.miyomu.data

import app.miyomu.data.db.MiyomuDatabase

interface MiyomuDatabaseFactory {
    fun createDatabase(): MiyomuDatabase
}

class MiyomuData internal constructor(
    private val database: MiyomuDatabase,
) {
    private val dao = database.dao()

    val library: LibraryRepository = RoomLibraryRepository(dao)
    val progress: ProgressRepository = RoomProgressRepository(dao)
    val anime: AnimeRepository = RoomAnimeRepository(dao)
    val chapters: ChapterRepository = RoomChapterRepository(dao)
    val videoResolve: VideoResolveRepository = RoomVideoResolveRepository(dao)
    val backup: BackupRepository = RoomBackupRepository(dao)
    val categories: CategoryRepository = RoomCategoryRepository(dao)

    fun close() {
        database.close()
    }
}

fun createMiyomuData(databaseFactory: MiyomuDatabaseFactory): MiyomuData =
    MiyomuData(databaseFactory.createDatabase())
