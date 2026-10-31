plugins {
    id("miyomu.kmp.library")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(project(":core:source"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

