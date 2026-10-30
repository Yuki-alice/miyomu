plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.source.manager"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.sourceApi)
            api(projects.shared.sourceRepo)
            implementation(projects.shared.sourceRuntime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
        }
    }
}
