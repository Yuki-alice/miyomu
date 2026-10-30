plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.source.novel"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.novel)
            implementation(projects.shared.domain)
            implementation(projects.shared.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmMain.dependencies {
            implementation(libs.ksoup)
            implementation(libs.quickjs.kt)
        }
        androidMain.dependencies {
            implementation(libs.ksoup.android)
            implementation(libs.quickjs.kt)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
