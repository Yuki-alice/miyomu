plugins {
    // AGP 9 built-in Kotlin 2.2.0 can read mediamp 0.5.1's Kotlin 2.3 metadata,
    // and satisfies Compose 1.12's AGP 9.1+/compileSdk 37 requirement.
    id("com.android.application") version "9.4.1"
    // Matches AGP 9 built-in Kotlin 2.2.0.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}

android {
    namespace = "org.projectv.spike.player.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.projectv.spike.player.android"
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
    implementation("org.openani.mediamp:mediamp-all:0.5.1")
}
