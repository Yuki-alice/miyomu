plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)

    jvm()

    android {
        namespace = "tachiyomi.presentation.core"
        compileSdk = 37
        minSdk = 26
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
            api(project(":shared:domain"))
            api(project(":shared:i18n"))
            api(project(":shared:i18n-aniyomi"))
            api(libs.moko.resources)
            api(libs.moko.resources.compose)
            api(libs.kotlinx.coroutines.core)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.kotlinx.collections.immutable)
        }
    }
}
