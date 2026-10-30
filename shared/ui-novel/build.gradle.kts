plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.novel"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.data)
            api(projects.shared.novel)
            api(projects.shared.sourceNovel)
            api(projects.shared.readerNovel)
            api(projects.shared.uiLibrary)
                        implementation(projects.shared.presentation)
            implementation(projects.shared.presentationCore)
            implementation(projects.shared.i18n)
implementation(libs.kotlinx.coroutines.core)
        }
    }
}
