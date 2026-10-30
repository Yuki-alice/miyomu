plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.model"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
    }
}
