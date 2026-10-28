plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
}

sourceSets["main"].resources.srcDir(rootProject.file("demo-resources"))

dependencies {
    implementation(project(":shared:source-runtime"))
    implementation(project(":shared:data"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
}

compose.desktop {
    application {
        mainClass = "app.miyomu.desktop.MainKt"
    }
}
