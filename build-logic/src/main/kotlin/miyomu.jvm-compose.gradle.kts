import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

configure<KotlinJvmProjectExtension> {
    jvmToolchain(MiyomuBuild.toolchain)
}

dependencies {
    implementation(compose.desktop.currentOs)
}
