plugins {
    id("miyomu.kmp.library")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            implementation(project(":core:model"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
