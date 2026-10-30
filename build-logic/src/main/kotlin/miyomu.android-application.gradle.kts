import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<ApplicationExtension> {
    compileSdk = MiyomuBuild.compileSdk

    defaultConfig {
        minSdk = MiyomuBuild.minSdk
        targetSdk = MiyomuBuild.targetSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}
