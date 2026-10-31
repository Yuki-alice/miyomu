plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            api(libs.material3.expressive)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

