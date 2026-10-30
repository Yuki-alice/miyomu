plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.network"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}
