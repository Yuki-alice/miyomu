plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.source.runtime"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.sourceApi)
            api(projects.shared.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
            implementation(libs.quickjs.kt)
        }
        jvmMain.dependencies {
            implementation(libs.ksoup)
        }
        androidMain.dependencies {
            implementation(libs.ksoup.android)
        }
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    useJUnitPlatform {
        if (!project.hasProperty("canary")) {
            excludeTags("canary")
        }
    }
}
