plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.reader.comic"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.local)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
