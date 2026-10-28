plugins {
    kotlin("multiplatform") version "2.4.10"
}

kotlin {
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.openani.mediamp:mediamp-api:0.5.1")
            implementation("org.openani.mediamp:mediamp-avkit:0.5.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
