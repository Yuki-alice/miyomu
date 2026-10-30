plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.source.api"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
