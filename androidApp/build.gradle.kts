plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.miyomu.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.miyomu.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
    implementation(project(":shared:ui"))
    implementation(project(":shared:local"))
    implementation(project(":shared:data"))
    implementation(project(":shared:network"))
    implementation(libs.ktor.client.core)
    implementation(libs.androidx.activity.compose)
}
