plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "app.miyomu.shared"
        compileSdk = 37
        minSdk = 26
    }
    jvm("desktop")
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "MiyomuShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:design"))
            implementation(project(":core:ui"))
            implementation(project(":feature:library"))
            implementation(project(":feature:discover"))
            implementation(project(":feature:reader"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:video"))
            implementation(project(":feature:downloads"))
            implementation(project(":feature:sync"))
            implementation(project(":feature:extensions"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.viewmodel)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
