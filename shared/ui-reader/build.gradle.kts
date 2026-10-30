plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.reader"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.download)
            api(projects.shared.sourceManager)
            api(projects.shared.data)
            api(projects.shared.readerComic)
            api(projects.shared.readerNovel)
            api(projects.shared.uiLibrary)
            api(projects.shared.uiEntries)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
