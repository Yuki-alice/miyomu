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
    implementation(compose.material3)
    implementation(libs.mediamp.all)
    // MPV native binaries are published separately from the player bundle.
    runtimeOnly("org.openani.mediamp:mediamp-mpv-runtime-macos-arm64:0.5.0")
}

val fixture = providers.gradleProperty("fixture").getOrElse("fixtures/base.mp4")
val subtitle = providers.gradleProperty("subtitle").getOrElse("fixtures/subtitle.srt")
val exitAfterMs = providers.gradleProperty("exitAfterMs").orNull
val autoTest = providers.gradleProperty("autoTest").orNull

compose.desktop {
    application {
        mainClass = "org.projectv.spike.player.MainKt"
        jvmArgs += buildList {
            add("-Dfixture.video=${project.layout.projectDirectory.file(fixture).asFile.absolutePath}")
            add("-Dfixture.subtitle=${project.layout.projectDirectory.file(subtitle).asFile.absolutePath}")
            if (exitAfterMs != null) add("-Dspike.exitAfterMs=$exitAfterMs")
            if (autoTest != null) add("-Dspike.autoTest=$autoTest")
        }
    }
}
