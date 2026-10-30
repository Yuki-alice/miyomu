plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.source.repo"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
