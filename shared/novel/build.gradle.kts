plugins {
    id("miyomu.kmp-library")
}

kotlin {
    android {
        namespace = "app.miyomu.novel"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            api(projects.shared.local)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(libs.ksoup)
        }
        androidMain.dependencies {
            implementation(libs.ksoup.android)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
