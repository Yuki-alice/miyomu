plugins {
    id("com.android.application") version "9.4.1"
    // Keep Compose compiler aligned with the Kotlin 2.3 line.
    // AGP 9 built-in Kotlin should resolve to the same 2.3.x compiler.
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
}

android {
    namespace = "app.miyomu.toolchainprobe"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.miyomu.toolchainprobe"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.github.dokar3:quickjs-kt:1.0.15")
    implementation("org.openani.mediamp:mediamp-all:0.5.1")
    implementation("io.ktor:ktor-client-okhttp:3.6.0")
}
