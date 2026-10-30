plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi")
    }
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())

    api(libs.kotlin.gradle.plugin)
    api(libs.android.gradle.plugin)
    api(libs.android.kmp.library.gradle.plugin)
    api(libs.android.application.gradle.plugin)
    api(libs.compose.gradle.plugin)
    api(libs.compose.compiler.gradle.plugin)

    implementation(kotlin("script-runtime"))
}
