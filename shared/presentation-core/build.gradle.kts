plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "tachiyomi.presentation.core"
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.domain)
            api(projects.shared.i18n)
            api(projects.shared.i18nAniyomi)
            api(libs.moko.resources)
            api(libs.moko.resources.compose)
            api(libs.kotlinx.coroutines.core)
            implementation(compose.animation)
            implementation(compose.uiUtil)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}
