plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.sources"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.data)
            api(projects.shared.sourceManager)
            api(projects.shared.sourceRepo)
            api(projects.shared.sourceNovel)
            api(projects.shared.download)
            api(projects.shared.uiLibrary)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
