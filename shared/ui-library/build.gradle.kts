plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.ui.library"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.uiCore)
            api(projects.shared.local)
            api(projects.shared.data)
                        implementation(projects.shared.presentation)
            implementation(projects.shared.presentationCore)
            implementation(projects.shared.i18n)
implementation(libs.kotlinx.coroutines.core)
        }
    }
}
