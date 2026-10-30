plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(project(":shared:local"))
    implementation(project(":shared:data"))
    implementation(project(":shared:network"))
    implementation(libs.ktor.client.core)
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
}

compose.desktop {
    application {
        mainClass = "app.miyomu.desktop.MainKt"
    }
}
