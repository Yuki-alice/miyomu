package app.miyomu.di

import app.miyomu.core.data.ContentRepository
import app.miyomu.core.data.DefaultContentRepository
import app.miyomu.core.data.DefaultLibraryRepository
import app.miyomu.core.data.LibraryRepository
import app.miyomu.core.database.MiyomuDatabase
import app.miyomu.core.source.MangaDexSourceRuntime
import app.miyomu.core.source.SourceRegistry
import app.miyomu.feature.discover.DetailsViewModel
import app.miyomu.feature.discover.DiscoverViewModel
import app.miyomu.feature.library.LibraryViewModel
import app.miyomu.feature.reader.ReaderViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(database: MiyomuDatabase): Module = module {
    single { database }
    single<LibraryRepository> { DefaultLibraryRepository(database) }
    single<ContentRepository> {
        DefaultContentRepository(
            SourceRegistry(listOf(MangaDexSourceRuntime())),
        )
    }

    viewModel { DiscoverViewModel(get()) }
    viewModel { DetailsViewModel(get(), get()) }
    viewModel { ReaderViewModel(get(), get()) }
    viewModel { LibraryViewModel(get()) }
}
