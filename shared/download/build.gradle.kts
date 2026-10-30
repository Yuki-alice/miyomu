plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.download"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.sourceManager)
            api(projects.shared.local)
            // The store implements the NovelChapterCache port for offline novel reading.
            implementation(projects.shared.novel)
            // Video episode downloads stream over the same Ktor client stack (P1-2).
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
