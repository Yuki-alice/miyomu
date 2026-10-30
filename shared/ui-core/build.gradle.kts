plugins {
    id("miyomu.kmp-compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.ui.core"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            api(projects.shared.contract)
            implementation(projects.shared.local)
            implementation(projects.shared.novel)
            implementation(projects.shared.readerComic)
            implementation(projects.shared.readerNovel)
            implementation(projects.shared.player)
            implementation(projects.shared.sourceRepo)
            implementation(projects.shared.sourceManager)
            implementation(projects.shared.download)
            implementation(projects.shared.danmaku)
            implementation(projects.shared.data)
            implementation(projects.shared.sourceNovel)
            api(projects.shared.presentationCore)
            api(projects.shared.presentation)
            api(projects.shared.i18n)
            api(projects.shared.i18nAniyomi)
            api(libs.navigation3.runtime)
            api(libs.coil.compose)
            api(libs.coil.network.okhttp)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
    }
}
