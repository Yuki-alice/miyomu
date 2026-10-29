@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    android {
        namespace = "app.miyomu.ui"
        compileSdk = 37
        minSdk = 26
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:model"))
            api(project(":shared:local"))
            api(project(":shared:reader-comic"))
            api(project(":shared:player"))
            api(project(":shared:source-repo"))
            api(project(":shared:source-manager"))
            api(project(":shared:data"))
            api(project(":shared:presentation-core"))
            // Ported Aniyomi screens (ADR-0011). Wired here so the module stays in the build
            // graph and cannot silently rot while the screens are connected one by one.
            api(project(":shared:presentation"))

            api(libs.voyager.navigator)
            api(libs.voyager.screenmodel)
            api(libs.voyager.tab.navigator)
            api(libs.voyager.transitions)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(compose.uiTest)
            implementation(libs.junit.jupiter)
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}
