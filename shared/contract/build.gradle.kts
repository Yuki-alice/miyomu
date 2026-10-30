plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.contract"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            api(projects.shared.domain)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
    }
}
