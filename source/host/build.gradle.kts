plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core:source"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}

kotlin {
    jvmToolchain(21)
}
