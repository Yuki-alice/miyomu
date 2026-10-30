package app.miyomu.data

import app.miyomu.contract.AnimeRepository
import app.miyomu.contract.BackupRepository
import app.miyomu.contract.CategoryRepository
import app.miyomu.contract.ChapterRepository
import app.miyomu.contract.DataPort
import app.miyomu.contract.LibraryRepository
import app.miyomu.contract.LibraryUpdateRepository
import app.miyomu.contract.ProgressRepository
import app.miyomu.contract.VideoResolveRepository
import app.miyomu.data.db.MiyomuDatabase

interface MiyomuDatabaseFactory {
    fun createDatabase(): MiyomuDatabase
}

class MiyomuData internal constructor(
    private val database: MiyomuDatabase,
) : DataPort {
    private val dao = database.dao()

    override val library: LibraryRepository = RoomLibraryRepository(dao)
    override val progress: ProgressRepository = RoomProgressRepository(dao)
    override val anime: AnimeRepository = RoomAnimeRepository(dao)
    override val chapters: ChapterRepository = RoomChapterRepository(dao)
    override val videoResolve: VideoResolveRepository = RoomVideoResolveRepository(dao)
    override val backup: BackupRepository = RoomBackupRepository(dao)
    override val categories: CategoryRepository = RoomCategoryRepository(dao)
    override val libraryUpdates: LibraryUpdateRepository = RoomLibraryUpdateRepository(dao)

    override fun close() {
        database.close()
    }
}

fun createMiyomuData(databaseFactory: MiyomuDatabaseFactory): MiyomuData =
    MiyomuData(databaseFactory.createDatabase())
