plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "tachiyomi.presentation.manga"
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xcontext-parameters",
        )
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.domain)
            api(projects.shared.presentationCore)
            api(projects.shared.i18n)
            api(projects.shared.i18nAniyomi)
            api(libs.voyager.screenmodel)
            api(libs.kotlinx.collections.immutable)
            implementation(compose.animation)
            implementation(compose.uiUtil)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.swipe)
            api(libs.coil.core)
            api(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
        }
    }
}
