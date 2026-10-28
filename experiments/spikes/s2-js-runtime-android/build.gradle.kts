plugins {
    // Standalone build. AGP 9.4's built-in Kotlin 2.2 cannot read quickjs-kt's
    // Kotlin 2.4 metadata, so this spike pins AGP 8.13 + Kotlin 2.4.21 + Gradle 8.13.
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.4.21"
}

android {
    namespace = "org.projectv.spike.js.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.projectv.spike.js.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].assets.srcDir("../s2-js-runtime/src/main/resources")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("io.github.dokar3:quickjs-kt:1.0.15")
    implementation("com.fleeksoft.ksoup:ksoup-android:0.2.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
