plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

kotlin {
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
            implementation(project(":core:source"))
            implementation(project(":core:model"))
            implementation(project(":core:data"))
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
