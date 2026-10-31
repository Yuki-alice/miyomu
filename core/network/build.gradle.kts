plugins {
    id("miyomu.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client.core)
            implementation(project(":core:model"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
