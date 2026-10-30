plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.entries"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.data)
            api(projects.shared.uiLibrary)
                        implementation(projects.shared.presentation)
implementation(libs.kotlinx.coroutines.core)
        }
    }
}
