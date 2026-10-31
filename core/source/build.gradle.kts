plugins {
    id("miyomu.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(project(":core:network"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.okio)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        sourceSets.matching { it.name.startsWith("ios") }.forEach { sourceSet ->
            sourceSet.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
