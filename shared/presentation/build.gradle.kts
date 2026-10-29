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
        namespace = "tachiyomi.presentation.manga"
        compileSdk = 37
        minSdk = 26
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
            // util/FastScrollAnimateItem.kt is copied verbatim from Aniyomi and uses a
            // `context(LazyItemScope)` parameter, which is still experimental in Kotlin 2.3.21.
            "-Xcontext-parameters",
        )
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            api(project(":shared:presentation-core"))
            api(project(":shared:i18n"))
            api(project(":shared:i18n-aniyomi"))

            api(libs.voyager.navigator)
            api(libs.voyager.screenmodel)
            api(libs.voyager.tab.navigator)
            api(libs.voyager.transitions)
            api(libs.kotlinx.collections.immutable)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.swipe)

            api(libs.coil.core)
            api(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
        }
    }
}
