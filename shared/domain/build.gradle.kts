plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "tachiyomi.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.i18n)
            api(projects.shared.i18nAniyomi)
            api(projects.shared.sourceApi)
            api(libs.moko.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
