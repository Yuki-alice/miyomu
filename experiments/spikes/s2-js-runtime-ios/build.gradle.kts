plugins {
    kotlin("multiplatform") version "2.4.10"
}

kotlin {
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.dokar3:quickjs-kt:1.0.15")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}
