plugins {
    id("miyomu.jvm.compose")
}


dependencies {
    implementation(project(":app:shared"))
    implementation(compose.desktop.currentOs)
    testImplementation(project(":core:ui"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "app.miyomu.desktop.MainKt"
    }
}
