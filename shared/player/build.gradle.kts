@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

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
        namespace = "app.miyomu.player"
        compileSdk = 37
        minSdk = 26
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }

    sourceSets {
        commonMain.dependencies {
            // mediamp-api carries both the player abstraction and the Compose surface, and has
            // no backend of its own. The `mediamp-all` bundle resolves to an empty JVM variant
            // in this setup, so the backends are declared per target instead (ADR-0005).
            api(libs.mediamp.api)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.mediamp.exoplayer)
        }
        jvmMain.dependencies {
            implementation(libs.mediamp.mpv)
            // macOS/Windows runtimes are Apache-2.0 + LGPL-2.1-or-later (dynamically linked,
            // replaceable). The Linux runtime and the aggregate artifact are GPL-3.0, so Linux
            // is deliberately left without a bundled runtime. See ADR-0012 (option A).
            runtimeOnly(libs.mediamp.mpv.runtime.macos.arm64)
            runtimeOnly(libs.mediamp.mpv.runtime.macos.x64)
            runtimeOnly(libs.mediamp.mpv.runtime.windows.x64)
            runtimeOnly(libs.mediamp.mpv.runtime.windows.arm64)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(compose.uiTest)
            // Compose UI tests on the JVM need the Skiko desktop natives.
            implementation(compose.desktop.currentOs)
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}
