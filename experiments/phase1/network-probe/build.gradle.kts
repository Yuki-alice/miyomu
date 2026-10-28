import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.21"
    id("com.android.kotlin.multiplatform.library") version "9.4.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
}

kotlin {
    jvmToolchain(21)

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    android {
        namespace = "app.miyomu.network"
        compileSdk = 37
        minSdk = 26

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.9.1")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.6.0")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:3.6.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter:5.14.4")
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}
