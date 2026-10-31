plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            api(project(":core:design"))
            implementation(project(":core:model"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.material.icons.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

