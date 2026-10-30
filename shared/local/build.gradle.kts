plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.local"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            implementation(libs.kotlinx.coroutines.core)
            // OpdsRemote fetches external feeds (P1-6); the engine is resolved at app level.
            implementation(libs.ktor.client.core)
        }
        // jvmShared compiles into both JVM targets, so the 7z reader is declared per target
        // (same pattern as ksoup/quickjs; hierarchy templates are AR-11 follow-up).
        jvmMain.dependencies {
            implementation(libs.commons.compress)
            implementation(libs.tukaani.xz)
            implementation(libs.pdfbox)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
        }
        androidMain.dependencies {
            implementation(libs.commons.compress)
            implementation(libs.tukaani.xz)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
