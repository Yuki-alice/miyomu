plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.settings"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.data)
            api(projects.shared.download)
            api(projects.shared.danmaku)
            api(projects.shared.network)
                        implementation(projects.shared.presentationCore)
            implementation(projects.shared.track)
implementation(libs.kotlinx.coroutines.core)
        }
    }
}
