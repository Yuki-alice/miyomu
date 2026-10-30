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
            // MiyomuApp's signature exposes PreferenceStore (domain) and it calls the canonical
            // theme/i18n directly, so declare what this module actually uses.
            api(projects.shared.domain)
            implementation(projects.shared.presentationCore)
            implementation(projects.shared.i18n)
            api(libs.navigation3.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
