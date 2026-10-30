plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.download"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.sourceManager)
            api(projects.shared.local)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
