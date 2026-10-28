plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.quickjs.kt)
    implementation(libs.kotlinx.coroutines)
    implementation("com.fleeksoft.ksoup:ksoup-jvm:0.2.6")
}

application {
    mainClass = "org.projectv.spike.js.MainKt"
}

tasks.register<JavaExec>("runFixtureServer") {
    group = "application"
    description = "Run the S2 fixture server on 127.0.0.1:18080"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.projectv.spike.js.FixtureServerMainKt")
    workingDir = rootProject.projectDir
}
