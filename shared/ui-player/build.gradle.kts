plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.player"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.local)
            api(projects.shared.data)
            api(projects.shared.danmaku)
            api(projects.shared.player)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
