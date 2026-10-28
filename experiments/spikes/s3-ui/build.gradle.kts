@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    // CMP 1.12.2 resolves stable Material3 1.9.0, which has no Expressive APIs.
    // Pin the alpha line explicitly to validate MD3E availability (see S3 report).
    implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
    implementation(compose.materialIconsExtended)

    testImplementation(compose.uiTest)
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "org.projectv.spike.MainKt"
    }
}
