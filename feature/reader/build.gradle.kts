plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:design"))
            implementation(project(":core:reader"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

