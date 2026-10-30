plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.player"
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.mediamp.api)
            api(projects.shared.danmaku)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.mediamp.exoplayer)
        }
        jvmMain.dependencies {
            implementation(libs.mediamp.mpv)
            runtimeOnly(libs.mediamp.mpv.runtime.macos.arm64)
            runtimeOnly(libs.mediamp.mpv.runtime.macos.x64)
            runtimeOnly(libs.mediamp.mpv.runtime.windows.x64)
            runtimeOnly(libs.mediamp.mpv.runtime.windows.arm64)
        }
    }
}
