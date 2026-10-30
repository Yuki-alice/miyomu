plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.data)
            api(projects.shared.uiLibrary)
            api(projects.shared.uiEntries)
            api(projects.shared.uiNovel)
            api(projects.shared.uiReader)
            api(projects.shared.uiSources)
            api(projects.shared.uiPlayer)
            api(projects.shared.uiSettings)
            api(libs.navigation3.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
